package com.speechpro.onepass.framework.model.tasks;

import com.speechpro.onepass.core.exception.CoreException;
import com.speechpro.onepass.core.sessions.transactions.VerificationTransaction;
import com.speechpro.onepass.framework.model.data.Video;

/**
 * @author volobuev
 * @since 16.06.16
 */
public class VerificationVideoTask extends ExceptionAsyncTask<Video, Void, Void> {

    private final VerificationTransaction verificationtransaction;

    public VerificationVideoTask(VerificationTransaction verificationtransaction) {
        super();
        this.verificationtransaction = verificationtransaction;
    }

    @Override
    protected Void doInBackground(Video... params) {
        try {
            Video video = params[0];
            verificationtransaction.addDynamicVerificationVideo(video.getVideo());
        } catch (CoreException e) {
            exception = e;
        }
        return null;
    }
}
