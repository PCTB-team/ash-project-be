package com.pctb.webapp.service;

// Scope nội bộ để service biết phải retrieve theo phạm vi nào.
public enum AiKnowledgeScope {
    // Chat trong một tài liệu.
    DOCUMENT,

    // Chat trong một folder.
    FOLDER,

    // Chat trên toàn bộ tài liệu của user.
    ALL_DOCUMENTS
}
