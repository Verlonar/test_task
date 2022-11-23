package ru.verlonar.test_task.model;

import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
public class EmailMessage {

    public EmailMessage(Long pdfFileLength, String text, String recipient) {
        this.pdfFileLength = pdfFileLength;
        this.text = text;
        this.recipient = recipient;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    private Long pdfFileLength;

    private String text;

    private String recipient;

    private LocalDateTime localDateTime;

    private boolean isDeliverySuccess;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        EmailMessage that = (EmailMessage) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
