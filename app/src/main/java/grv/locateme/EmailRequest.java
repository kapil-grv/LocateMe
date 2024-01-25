package grv.locateme;

public class EmailRequest {
    String to;
    String subject;
    String body;

    public EmailRequest(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
    }
}
