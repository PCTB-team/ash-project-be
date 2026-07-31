alter table payment_transaction
    add column expired_at datetime(6) null;

update payment_transaction
set expired_at = date_add(created_at, interval 30 second)
where expired_at is null
  and status = 'PENDING'
  and created_at is not null;

create index idx_payment_transaction_status_expired_at
    on payment_transaction (status, expired_at);
