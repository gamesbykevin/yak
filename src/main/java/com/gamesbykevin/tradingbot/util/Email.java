package com.gamesbykevin.tradingbot.util;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static com.gamesbykevin.tradingbot.util.LogFile.FILE_SEPARATOR;
import static com.gamesbykevin.tradingbot.util.LogFile.getFilenameEmail;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

public class Email implements Runnable {

    public static String EMAIL_NOTIFICATION_ADDRESS = null;

    public static String GMAIL_SMTP_USERNAME = null;
    public static String GMAIL_SMTP_PASSWORD = null;

    private final String subject, text;

    private Email(final String subject, final String text) {
        this.subject = subject;
        this.text = text;
    }

    //where we write our log file(s)
    private static PrintWriter WRITER;

    @Override
    public void run() {

        try {

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(GMAIL_SMTP_USERNAME, GMAIL_SMTP_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(GMAIL_SMTP_USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_NOTIFICATION_ADDRESS));
            message.setSubject(subject);
            message.setText(text);

            //we are now sending
            displayMessage("Sending email....", getWriter());

            //display email information
            displayMessage("From: " + GMAIL_SMTP_USERNAME, getWriter());
            displayMessage("To: " + EMAIL_NOTIFICATION_ADDRESS, getWriter());

            if (subject != null)
                displayMessage("Subject: " + subject, getWriter());

            if (text != null)
                displayMessage("Message: " + text, getWriter());

            //send the email
            Transport.send(message);

            //display we are good
            displayMessage("Sent email successfully....", getWriter());

        } catch (Exception e) {

            //print and write error to log
            displayMessage(e, getWriter());
        }
    }

    public static boolean hasContactAddress() {

        //we can't send if null or too short
        if (EMAIL_NOTIFICATION_ADDRESS == null || EMAIL_NOTIFICATION_ADDRESS.trim().length() < 10)
            return false;

        //we are good
        return true;
    }

    public static synchronized void sendEmail(final String subject, final String text) {

        //don't send email if email address doesn't exist
        if (!hasContactAddress())
            return;

        //create our email object
        Email email = new Email(subject, text);

        //we will send the email on a separate thread
        Thread thread = new Thread(email);

        //start sending the email on a separate thread
        thread.start();
    }

    public static String getTextDateDesc() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public static String getFileDateDesc() {
        return new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date()).replaceAll(" ", "_");
    }

    protected static final PrintWriter getWriter() {

        //create the main log file and place in our root logs directory
        if (WRITER == null)
            WRITER = LogFile.getPrintWriter(getFilenameEmail(), LogFile.getLogDirectory());

        return WRITER;
    }
}