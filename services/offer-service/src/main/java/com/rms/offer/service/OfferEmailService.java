package com.rms.offer.service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.rms.offer.client.OfferEmailContextClient.Context;
import com.rms.offer.entity.Offer;

@Service
public class OfferEmailService {
    private final JavaMailSender sender;
    private final String from;

    public OfferEmailService(JavaMailSender sender, @Value("${rms.mail.from:}") String from) {
        this.sender = sender;
        this.from = from;
    }

    public void send(Offer offer, Context context) {
        if (!StringUtils.hasText(from)) throw new IllegalStateException("SMTP_FROM chưa được cấu hình");
        if (context == null || !StringUtils.hasText(context.candidateEmail()))
            throw new IllegalStateException("Hồ sơ ứng tuyển chưa có email ứng viên");
        String candidate = value(context.candidateName(), "bạn");
        String title = value(context.jobTitle(), "Vị trí tuyển dụng");
        String company = value(context.companyName(), "Công ty");
        String currency = value(offer.getCurrency(), "VND");
        String start = offer.getStartDate() == null ? "Theo thỏa thuận" : offer.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")));
        String expiry = offer.getExpiryDate() == null ? "Không ghi rõ" : offer.getExpiryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")));
        String notes = StringUtils.hasText(offer.getNotes()) ? "\n\nGhi chú thêm: " + offer.getNotes().trim() : "";
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(context.candidateEmail());
        mail.setSubject("Thư mời nhận việc – " + title + " tại " + company);
        mail.setText("Chào " + candidate + ",\n\n"
                + "Chúc mừng bạn đã vượt qua các vòng tuyển chọn cho vị trí " + title + " tại " + company
                + ". Chúng tôi trân trọng mời bạn gia nhập đội ngũ với các thông tin sau:\n\n"
                + "Mức lương đề xuất: " + offer.getProposedSalary().toPlainString() + " " + currency + "\n"
                + "Ngày dự kiến bắt đầu: " + start + "\n"
                + "Hạn phản hồi thư mời: " + expiry + notes + "\n\n"
                + "Vui lòng đăng nhập vào hệ thống để xem chi tiết và phản hồi thư mời trước hạn nêu trên. "
                + "Nếu cần trao đổi thêm, bạn có thể trả lời email này.\n\n"
                + "Mong sớm được chào đón bạn tại " + company + ".\n\n"
                + "Trân trọng,\nBộ phận Tuyển dụng\n" + company);
        sender.send(mail);
    }

    private String value(String value, String fallback) { return StringUtils.hasText(value) ? value.trim() : fallback; }
}
