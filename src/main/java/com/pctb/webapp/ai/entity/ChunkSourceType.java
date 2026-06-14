package com.pctb.webapp.ai.entity;

/**
 * Cho biet chunk text duoc tao tu document ca nhan hay file trong group.
 */
public enum ChunkSourceType {
    // Chunk den tu bang Document cua user.
    PERSONAL_DOCUMENT,

    // Chunk den tu bang GroupFile cua study group.
    GROUP_FILE
}
