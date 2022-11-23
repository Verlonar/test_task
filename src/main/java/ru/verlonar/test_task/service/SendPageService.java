package ru.verlonar.test_task.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.verlonar.test_task.model.EmailMessage;
import ru.verlonar.test_task.repository.MessageRepository;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Properties;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

@Service
public class SendPageService {

    private final MessageRepository messageRepository;

    @Value("${path.to.pdfFiles.folder}")
    private String pdfFilesDir;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${spring.mail.password}")
    private String password;
    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${path.to.font.supported.utf.8}")
    private String pathToFontSupportedUtf8;




    public SendPageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Подготавливает все данные для отправки сообщения и сохранения его в базу данных
     * @param pdfFile файл, который загрузил пользователь
     * @param text текст, указанный пользователем при отправке файла
     * @param recipient адрес электронной почты получателя
     */
    public void messageManager(MultipartFile pdfFile, String text, String recipient) {

        String originalPdfFileName = pdfFile.getOriginalFilename();

        assert originalPdfFileName != null;

        String pdfFileNameToSave = recipient + "." + getExtensions(originalPdfFileName);

        Path originalFilePath = saveFileAndReturnPath(pdfFile, pdfFileNameToSave);
        Path modifyingPdfFilePath = modifyPdfFileAndReturnNewPath(pdfFileNameToSave, text, originalFilePath);

        EmailMessage emailMessage = new EmailMessage(pdfFile.getSize(), text, recipient);

        LocalDateTime dispatchTime = LocalDateTime.now();

        try {
            Properties properties = getProperties();

            Session session = Session.getInstance(properties, null);

            MimeMessage message = createMessage(session, recipient, dispatchTime, text, modifyingPdfFilePath);

            sendMessage(message, session);

            saveMessageToDb(emailMessage, dispatchTime, true);

        } catch (Exception e) {
            saveMessageToDb(emailMessage, dispatchTime, false);
            e.printStackTrace();
        }
    }

    /**
     * Принимает отправленный пользователем Pdf файл,
     * сохраняет его и возвращает путь к сохраненному файлу
     * @param pdfFile файл, который загрузил пользователь
     * @param pdfFileNameToSave имя сохраненного файла, который загрузил пользователь
     * @return Path
     */
    private Path saveFileAndReturnPath(MultipartFile pdfFile, String pdfFileNameToSave) {

        Path filePath = null;

        try {
            filePath = Path.of(pdfFilesDir, pdfFileNameToSave);
            Files.createDirectories(filePath.getParent());
            Files.deleteIfExists(filePath);

            try (InputStream fis = pdfFile.getInputStream();
                 OutputStream fos = Files.newOutputStream(filePath, CREATE_NEW);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)
            ) {
                bis.transferTo(bos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filePath;
    }

    /**
     * Принимает путь к отправленному пользователем Pdf файлу,
     * накладывает на него полученный от пользователя текст,
     * сохраняет модифицированный файл и возвращает путь к модифицированному файлу
     * @param pdfFileNameToSave имя сохраненного файла, который загрузил пользователь
     * @param text текст, указанный пользователем при отправке файла
     * @param filePath путь к файлу, который загрузил пользователь
     * @return Path
     */
    private Path modifyPdfFileAndReturnNewPath(String pdfFileNameToSave,String text, Path filePath) {

        Path modifyingPdfFilePath = null;

        try {
            String modifyingPdfFileName = "Modifying-" + pdfFileNameToSave;
            modifyingPdfFilePath = Path.of(pdfFilesDir, modifyingPdfFileName);

            PdfReader reader = new PdfReader(filePath.toString());
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(modifyingPdfFilePath.toString()));
            BaseFont bf = BaseFont.createFont(pathToFontSupportedUtf8, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {

                PdfContentByte over = stamper.getOverContent(i);

                over.beginText();
                over.setColorFill(BaseColor.GREEN);
                over.setFontAndSize(bf, 50);
                over.setTextMatrix(0, 680);
                over.showText(text);
                over.endText();
                stamper.setFullCompression();
            }

            stamper.close();
            reader.close();
        }   catch (Exception e) {
            e.printStackTrace();
        }

        return modifyingPdfFilePath;
    }

    /**
     * Возвращет расширение файла, который загрузил пользователь
     * @param fileName имя файла
     * @return String
     */
    private String getExtensions(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * Сохранение объекта EmailMessage базу данных
     * Добавляет в объект время отправки, статус отправлено/не отправлено
     * и сохраняет объект в базу данных
     * @param message объект класса EmailMessage
     * @param dispatchTime время отправки сообщения
     * @param isDelivery статус отправления отправлено/не отправлено
     */
    private void saveMessageToDb(EmailMessage message, LocalDateTime dispatchTime, boolean isDelivery) {
        message.setLocalDateTime(dispatchTime);
        message.setDeliverySuccess(isDelivery);

        messageRepository.save(message);
    }

    /**
     * Подготавливает и возвращает настройки для отправки сообщения
     * @return Properties
     */
    private Properties getProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", mailHost);
        properties.put("mail.from", from);
        properties.put("mail.smtp.starttls.enable", "true");
        return properties;
    }

    /**
     * Формирует и возвращает сообщение для отправки
     * @param session сессия для отправки сообщения
     * @param recipient адрес электронной почты получателя
     * @param dispatchTime время отправки сообщения
     * @param text текст, указанный пользователем при отправке файла
     * @param modifyingPdfFilePath путь к модифицированному файлу
     * @return MimeMessage
     * @throws MessagingException пробрасывает в случе ошибки при формировании сообщения
     * @throws UnsupportedEncodingException
     */
    private MimeMessage createMessage(Session session,
                                      String recipient,
                                      LocalDateTime dispatchTime,
                                      String text,
                                      Path modifyingPdfFilePath) throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = new MimeMessage(session);
        message.setRecipients(Message.RecipientType.TO, recipient);
        message.setSubject(dispatchTime.toString());
        message.setText(text);

        BodyPart textBodyPart = new MimeBodyPart();
        textBodyPart.setText(text);

        MimeBodyPart fileBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(new File(modifyingPdfFilePath.toUri()));
        fileBodyPart.setDataHandler(new DataHandler(source));
        fileBodyPart.setFileName(MimeUtility.encodeText(source.getName()));

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(textBodyPart);
        multipart.addBodyPart(fileBodyPart);

        message.setContent(multipart);
        return message;
    }

    /**
     * Отправляет сообщение по электронной почте
     * @param message подготовленное сообщение, содержащее все данные для отправки
     * @param session сессия для отправки сообщения
     * @throws MessagingException пробрасывает в случае если сообщение не отправлено
     */
    private void sendMessage(MimeMessage message, Session session) throws MessagingException{
        Transport tr = session.getTransport();
        tr.connect(from, password);
        tr.sendMessage(message, message.getAllRecipients());
        tr.close();
    }
}
