package ru.verlonar.test_task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.verlonar.test_task.model.EmailMessage;

public interface MessageRepository extends JpaRepository<EmailMessage, Long> {

}
