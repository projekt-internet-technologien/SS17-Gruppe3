/** 
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use this file 
 * except in compliance with the License. A copy of the License is located at
 *
 *   http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package com.amazon.alexa.avs;

import com.amazon.alexa.avs.http.MultipartParser.MultipartParserConsumer;
import com.amazon.alexa.avs.message.Payload;
import com.amazon.alexa.avs.message.response.AttachedContentPayload;
import com.amazon.alexa.avs.message.response.Directive;

import java.io.InputStream;
import java.util.*;

/**
 * The DirectiveEnqueuer takes parts parsed from a multipart parser, combines directves with their
 * attached content, and triages those directives into either the dependent directive queue or
 * independent directive queue.
 *
 * Any directive with the current dialogRequestID is dependent on all the directives with that id
 * which came before it. These directives are added to the dependent directive queue. Any directive
 * with no dialogRequestId is dependent on nothing and is added to the independent directive queue.
 */
public class DirectiveEnqueuer implements MultipartParserConsumer {

    // The authority for the current dialogRequestId.
    private final DialogRequestIdAuthority dialogRequestIdAuthority;

    // Queue made up of all dependent directives for the current dialogRequestId
    private final Queue<Directive> dependentQueue;

    // Queue made up of all directives without a dialogRequestId
    private final Queue<Directive> independentQueue;

    // Queue for incomplete directives. A directive is incomplete if it still needs some attached
    // content to be associated with it.
    private final Queue<Directive> incompleteDirectiveQueue;

    // Map of all attachments which have not yet been matched with directives.
    private final Map<String, InputStream> attachments;

    public DirectiveEnqueuer(DialogRequestIdAuthority dialogRequestIdAuthority,
            Queue<Directive> dependentQueue, Queue<Directive> independentQueue) {
        this.dialogRequestIdAuthority = dialogRequestIdAuthority;
        this.dependentQueue = dependentQueue;
        this.independentQueue = independentQueue;
        incompleteDirectiveQueue = new LinkedList<>();
        attachments = new HashMap<>();
    }

    @Override
    public synchronized void onDirective(Directive directive) {
        incompleteDirectiveQueue.add(directive);
        matchAttachementsWithDirectives();
    }

    @Override
    public synchronized void onDirectiveAttachment(String contentId,
            InputStream attachmentContent) {
        attachments.put(contentId, attachmentContent);
        matchAttachementsWithDirectives();
    }

    private void matchAttachementsWithDirectives() {
        for (Directive directive : incompleteDirectiveQueue) {
            Payload payload = directive.getPayload();
            if (payload instanceof AttachedContentPayload) {
                AttachedContentPayload attachedContentPayload = (AttachedContentPayload) payload;
                String contentId = attachedContentPayload.getAttachedContentId();

                InputStream attachment = attachments.remove(contentId);
                if (attachment != null) {
                    attachedContentPayload.setAttachedContent(contentId, attachment);
                }
            }
        }

        findCompleteDirectives();
    }

    private void findCompleteDirectives() {
        Iterator<Directive> iterator = incompleteDirectiveQueue.iterator();
        while (iterator.hasNext()) {
            Directive directive = iterator.next();
            Payload payload = directive.getPayload();
            if (payload instanceof AttachedContentPayload) {
                AttachedContentPayload attachedContentPayload = (AttachedContentPayload) payload;

                if (!attachedContentPayload.requiresAttachedContent()) {
                    // The front most directive IS complete.
                    enqueueDirective(directive);
                    iterator.remove();
                } else {
                    break;
                }
            } else {
                // Immediately enqueue any directive which does not contain audio content
                enqueueDirective(directive);
                iterator.remove();
            }
        }
    }

    private void enqueueDirective(Directive directive) {
        String dialogRequestId = directive.getDialogRequestId();
        if (dialogRequestId == null) {
            independentQueue.add(directive);
        } else if (dialogRequestIdAuthority.isCurrentDialogRequestId(dialogRequestId)) {
            dependentQueue.add(directive);
        }
    }
}
