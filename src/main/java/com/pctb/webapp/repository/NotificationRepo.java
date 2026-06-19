package com.pctb.webapp.repository;

import com.pctb.webapp.entity.Notification;
import com.pctb.webapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface NotificationRepo extends JpaRepository<Notification, String> {
    Page<Notification> findByReceiverUserOrderByCreatedAtDesc(User receiverUser, Pageable pageable);

    Page<Notification> findByReceiverUserAndReadOrderByCreatedAtDesc(User receiverUser, Boolean read, Pageable pageable);

    long countByReceiverUserAndReadFalse(User receiverUser);

    List<Notification> findByIdInAndReceiverUser(Collection<String> ids, User receiverUser);

    List<Notification> findByReceiverUserAndReadFalse(User receiverUser);
}
