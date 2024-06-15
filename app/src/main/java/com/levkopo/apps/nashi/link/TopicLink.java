package com.levkopo.apps.nashi.link;

public class TopicLink extends AbsInternalLink {

    public int replyToOwner;
    public int topicOwnerId;
    public int replyToCommentId;

    @Override
    public String toString() {
        return "TopicLink{" +
			"replyToOwner=" + replyToOwner +
			", topicOwnerId=" + topicOwnerId +
			", replyToCommentId=" + replyToCommentId +
			"} " + super.toString();
    }
}
