package ru.verlonar.test_task.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.verlonar.test_task.service.SendPageService;


@Controller
@RequestMapping("/send")
public class SendPageController {

    private final SendPageService sendPageService;

    public SendPageController(SendPageService sendPageService) {
        this.sendPageService = sendPageService;
    }

    @PostMapping
    public String sendMessage(@RequestParam("pdfFile") MultipartFile pdfFile,
                            @RequestParam("text") String text,
                            @RequestParam("recipient") String recipient) {

        sendPageService.messageManager(pdfFile, text, recipient);
        return "redirect:/";
    }
}
