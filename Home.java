import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import com.mongodb.client.*;
import org.bson.Document;

public class Home extends JFrame {
    private JTabbedPane tabbedPane;
    private JPanel loginPanel, signupPanel, topPanel;
    private boolean isDarkMode = false;

    // Signup fields
    private JTextField nameField, emailField, usernameFieldSignup;
    private JPasswordField passwordFieldSignup, confirmPasswordField;
    private JButton signupButton;

    // OTP Verification
    private String generatedOTP;
    private String userEmailTemp;

    // Login fields
    private JTextField usernameFieldLogin;
    private JPasswordField passwordFieldLogin;
    private JButton loginButton;

    private JButton toggleThemeButton;
    
    // Colors for the UI
    private final Color PRIMARY_COLOR = new Color(63, 81, 181);  // Material Design Indigo
    private final Color SUCCESS_COLOR = new Color(76, 175, 80);  // Material Green
    private final Color DARK_BG = new Color(33, 33, 33);
    private final Color DARK_FIELD_BG = new Color(50, 50, 50);
    
    // UI constants
    private final int BORDER_RADIUS = 15;
    private final int FIELD_HEIGHT = 35;

    // Email configuration - Use the same credentials for both functions
    private final String EMAIL_FROM = "prashaantv05@gmail.com";
    private final String EMAIL_APP_PASSWORD = "ytcogjntukatsusf"; // Use consistent app password

    public Home() {
        setTitle("Task Manager - Home");
        setSize(600, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        setupLoginPanel();
        setupSignupPanel();

        tabbedPane.add("Login", loginPanel);
        tabbedPane.add("Signup", signupPanel);

        toggleThemeButton = createButton("Switch to Dark Mode", PRIMARY_COLOR, Color.WHITE);
        toggleThemeButton.addActionListener(e -> toggleTheme());

        topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
        topPanel.add(toggleThemeButton, BorderLayout.EAST);

        setLayout(new BorderLayout(10, 10));
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        // Add a logo or app name at the bottom
        JLabel appCreditsLabel = new JLabel("TaskMaster © 2025", SwingConstants.CENTER);
        appCreditsLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        add(appCreditsLabel, BorderLayout.SOUTH);

        applyTheme();
        setVisible(true);
    }

    private void setupLoginPanel() {
        loginPanel = new JPanel();
        loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
        loginPanel.setBorder(new EmptyBorder(30, 50, 30, 50));

        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("Welcome to");
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel appNameLabel = new JLabel("TaskMaster");
        appNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        appNameLabel.setForeground(PRIMARY_COLOR);
        appNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        logoPanel.add(titleLabel);
        logoPanel.add(Box.createVerticalStrut(5));
        logoPanel.add(appNameLabel);
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBorder(new EmptyBorder(25, 0, 15, 0));
        
        usernameFieldLogin = createStyledField("Username");
        passwordFieldLogin = new JPasswordField();
        setupFieldStyle(passwordFieldLogin, "Password");
        
        loginButton = createButton("LOGIN", PRIMARY_COLOR, Color.WHITE);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(200, 40));
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 16));

        loginButton.addActionListener(e -> {
            String user = usernameFieldLogin.getText();
            String pass = new String(passwordFieldLogin.getPassword());
            
            if (user.isEmpty() || pass.isEmpty()) {
                showError("Please enter both username and password.");
                return;
            }

            try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
                MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
                MongoCollection<Document> collection = db.getCollection("users");

                Document query = new Document("username", user).append("password", pass);
                Document result = collection.find(query).first();

                if (result != null) {
                    JOptionPane.showMessageDialog(this, "Login Successful!", "Success", 
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    new TaskList(user);
                } else {
                    showError("Invalid username or password.");
                }
            } catch (Exception ex) {
                showError("Database Error: " + ex.getMessage());
            }
        });

        fieldsPanel.add(usernameFieldLogin);
        fieldsPanel.add(Box.createVerticalStrut(15));
        fieldsPanel.add(passwordFieldLogin);
        fieldsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Create forgotPassword link
        JLabel forgotPasswordLabel = new JLabel("Forgot Password?");
        forgotPasswordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        forgotPasswordLabel.setForeground(PRIMARY_COLOR);
        forgotPasswordLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotPasswordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add functionality to forgotPasswordLabel
        forgotPasswordLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showForgotPasswordDialog();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                forgotPasswordLabel.setText("<html><u>Forgot Password?</u></html>");
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                forgotPasswordLabel.setText("Forgot Password?");
            }
        });
        
        loginPanel.add(logoPanel);
        loginPanel.add(fieldsPanel);
        loginPanel.add(Box.createVerticalStrut(10));
        loginPanel.add(loginButton);
        loginPanel.add(Box.createVerticalStrut(15));
        loginPanel.add(forgotPasswordLabel);
    }

    private void showForgotPasswordDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel label = new JLabel("Enter your email address:");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField emailField = new JTextField(20);
        emailField.setMaximumSize(new Dimension(300, 30));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        panel.add(emailField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
                "Password Recovery", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                
        if (result == JOptionPane.OK_OPTION) {
            String email = emailField.getText();
            
            if (email == null || email.trim().isEmpty()) {
                showError("Please enter your email address.");
                return;
            }
            
            if (!isValidEmail(email)) {
                showError("Please enter a valid email address.");
                return;
            }
            
            // Check if email exists in database
            try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
                MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
                MongoCollection<Document> collection = db.getCollection("users");
                
                Document user = collection.find(new Document("email", email)).first();
                
                if (user == null) {
                    showError("Email address not found in our records.");
                    return;
                }
                
                // Generate OTP for password reset
                userEmailTemp = email;
                generatedOTP = String.format("%06d", new Random().nextInt(999999));
                
                // Send OTP
                sendEmail(email, generatedOTP, "Password Reset");
                
                // Verify OTP and allow password reset
                verifyOTPAndResetPassword(email);
            } catch (Exception ex) {
                showError("Database Error: " + ex.getMessage());
            }
        }
    }
    
    private void verifyOTPAndResetPassword(String email) {
        JPanel otpPanel = new JPanel();
        otpPanel.setLayout(new BoxLayout(otpPanel, BoxLayout.Y_AXIS));
        
        JLabel otpMessage = new JLabel("Enter the 6-digit code sent to your email:");
        otpMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField otpField = new JTextField(10);
        otpField.setMaximumSize(new Dimension(200, 30));
        otpField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        otpPanel.add(otpMessage);
        otpPanel.add(Box.createVerticalStrut(10));
        otpPanel.add(otpField);
        
        int result = JOptionPane.showConfirmDialog(this, otpPanel, 
                "OTP Verification", JOptionPane.OK_CANCEL_OPTION);
                
        if (result == JOptionPane.OK_OPTION) {
            String enteredOTP = otpField.getText();
            if (enteredOTP != null && enteredOTP.equals(generatedOTP)) {
                // OTP verified, now show password reset dialog
                showNewPasswordDialog(email);
            } else {
                showError("Incorrect verification code. Please try again.");
            }
        }
    }
    
    private void showNewPasswordDialog(String email) {
        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.Y_AXIS));
        
        JLabel passwordLabel = new JLabel("Enter new password:");
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPasswordField newPasswordField = new JPasswordField(20);
        newPasswordField.setMaximumSize(new Dimension(300, 30));
        newPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel confirmLabel = new JLabel("Confirm new password:");
        confirmLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPasswordField confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setMaximumSize(new Dimension(300, 30));
        confirmPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        passwordPanel.add(passwordLabel);
        passwordPanel.add(Box.createVerticalStrut(5));
        passwordPanel.add(newPasswordField);
        passwordPanel.add(Box.createVerticalStrut(10));
        passwordPanel.add(confirmLabel);
        passwordPanel.add(Box.createVerticalStrut(5));
        passwordPanel.add(confirmPasswordField);
        
        int result = JOptionPane.showConfirmDialog(this, passwordPanel, 
                "Reset Password", JOptionPane.OK_CANCEL_OPTION);
                
        if (result == JOptionPane.OK_OPTION) {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (newPassword.isEmpty()) {
                showError("Password cannot be empty.");
                showNewPasswordDialog(email);
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                showError("Passwords do not match. Please try again.");
                showNewPasswordDialog(email);
                return;
            }
            
            // Update password in database
            try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
                MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
                MongoCollection<Document> collection = db.getCollection("users");
                
                Document query = new Document("email", email);
                Document update = new Document("$set", new Document("password", newPassword));
                
                collection.updateOne(query, update);
                
                JOptionPane.showMessageDialog(this, 
                        "Password has been reset successfully. You can now login with your new password.", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showError("Database Error: " + ex.getMessage());
            }
        }
    }

    private void setupSignupPanel() {
        signupPanel = new JPanel();
        signupPanel.setLayout(new BoxLayout(signupPanel, BoxLayout.Y_AXIS));
        signupPanel.setBorder(new EmptyBorder(20, 50, 20, 50));

        JLabel signupTitle = new JLabel("Create an Account");
        signupTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        signupTitle.setForeground(PRIMARY_COLOR);
        signupTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        nameField = createStyledField("Full Name");
        emailField = createStyledField("Email Address");
        usernameFieldSignup = createStyledField("Choose Username");
        
        passwordFieldSignup = new JPasswordField();
        setupFieldStyle(passwordFieldSignup, "Password");
        
        confirmPasswordField = new JPasswordField();
        setupFieldStyle(confirmPasswordField, "Confirm Password");

        signupButton = createButton("REGISTER", PRIMARY_COLOR, Color.WHITE);
        signupButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signupButton.setMaximumSize(new Dimension(200, 40));
        signupButton.setFont(new Font("Segoe UI", Font.BOLD, 16));

        signupButton.addActionListener(e -> {
            String name = nameField.getText();
            String email = emailField.getText();
            String username = usernameFieldSignup.getText();
            String password = new String(passwordFieldSignup.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (name.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                showError("Please fill all fields.");
                return;
            }

            if (!isValidEmail(email)) {
                showError("Please enter a valid email address.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                showError("Passwords do not match.");
                return;
            }
            
            try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
                MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
                MongoCollection<Document> collection = db.getCollection("users");
                
                // Check if username already exists
                Document existingUser = collection.find(new Document("username", username)).first();
                if (existingUser != null) {
                    showError("Username already exists. Please choose another one.");
                    return;
                }
                
                // Check if email already exists
                Document existingEmail = collection.find(new Document("email", email)).first();
                if (existingEmail != null) {
                    showError("Email address already registered.");
                    return;
                }
            } catch (Exception ex) {
                showError("Error checking database: " + ex.getMessage());
                return;
            }

            userEmailTemp = email;
            generatedOTP = String.format("%06d", new Random().nextInt(999999));
            
            int response = JOptionPane.showConfirmDialog(this, 
                    "We'll send a verification code to " + email + ". Continue?", 
                    "Confirm Email", JOptionPane.YES_NO_OPTION);
                    
            if (response == JOptionPane.YES_OPTION) {
                sendEmail(email, generatedOTP, "Email Verification");
                verifyOTPAndRegister(name, email, username, password);
            }
        });

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(15, 0, 15, 0));
        formPanel.add(nameField);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(emailField);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(usernameFieldSignup);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(passwordFieldSignup);
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(confirmPasswordField);
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        signupPanel.add(signupTitle);
        signupPanel.add(Box.createVerticalStrut(15));
        signupPanel.add(formPanel);
        signupPanel.add(Box.createVerticalStrut(15));
        signupPanel.add(signupButton);
    }
    
    private boolean isValidEmail(String email) {
        String regex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return email.matches(regex);
    }
    
    private void verifyOTPAndRegister(String name, String email, String username, String password) {
        JPanel otpPanel = new JPanel();
        otpPanel.setLayout(new BoxLayout(otpPanel, BoxLayout.Y_AXIS));
        
        JLabel otpMessage = new JLabel("Enter the 6-digit code sent to your email:");
        otpMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField otpField = new JTextField(10);
        otpField.setMaximumSize(new Dimension(200, 30));
        otpField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        otpPanel.add(otpMessage);
        otpPanel.add(Box.createVerticalStrut(10));
        otpPanel.add(otpField);
        
        int result = JOptionPane.showConfirmDialog(this, otpPanel, 
                "OTP Verification", JOptionPane.OK_CANCEL_OPTION);
                
        if (result == JOptionPane.OK_OPTION) {
            String enteredOTP = otpField.getText();
            if (enteredOTP != null && enteredOTP.equals(generatedOTP)) {
                try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
                    MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
                    MongoCollection<Document> collection = db.getCollection("users");

                    Document doc = new Document("username", username)
                            .append("password", password)
                            .append("email", email)
                            .append("name", name)
                            .append("createdAt", new Date());

                    collection.insertOne(doc);
                    JOptionPane.showMessageDialog(this, "Registration successful! You can now login.", 
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearSignupFields();
                    tabbedPane.setSelectedIndex(0);
                } catch (Exception ex) {
                    showError("Database Error: " + ex.getMessage());
                }
            } else {
                showError("Incorrect verification code. Please try again.");
            }
        }
    }
    
    private void clearSignupFields() {
        nameField.setText("");
        emailField.setText("");
        usernameFieldSignup.setText("");
        passwordFieldSignup.setText("");
        confirmPasswordField.setText("");
    }

    // Unified email sending method
    private void sendEmail(String toEmail, String otp, String purpose) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        try {
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_APP_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            
            if (purpose.equals("Email Verification")) {
                message.setSubject("TaskMaster - Email Verification");
                String htmlContent = 
                    "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto;'>" +
                    "<h2 style='color: #3F51B5;'>TaskMaster Verification Code</h2>" +
                    "<p>Hello,</p>" +
                    "<p>Thank you for registering with TaskMaster. Use the following code to verify your email address:</p>" +
                    "<div style='background-color: #f5f5f5; padding: 15px; font-size: 24px; font-weight: bold; text-align: center; letter-spacing: 5px;'>" + 
                    otp + "</div>" +
                    "<p>This code will expire in 10 minutes.</p>" +
                    "<p>If you didn't request this code, please ignore this email.</p>" +
                    "<p>Regards,<br>TaskMaster Team</p>" +
                    "</div>";
                message.setContent(htmlContent, "text/html; charset=utf-8");
            } else {
                message.setSubject("TaskMaster - Password Reset");
                String htmlContent = 
                    "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto;'>" +
                    "<h2 style='color: #3F51B5;'>TaskMaster Password Reset</h2>" +
                    "<p>Hello,</p>" +
                    "<p>You requested to reset your password. Use the following verification code:</p>" +
                    "<div style='background-color: #f5f5f5; padding: 15px; font-size: 24px; font-weight: bold; text-align: center; letter-spacing: 5px;'>" + 
                    otp + "</div>" +
                    "<p>This code will expire in 10 minutes.</p>" +
                    "<p>If you didn't request a password reset, please ignore this email or contact support.</p>" +
                    "<p>Regards,<br>TaskMaster Team</p>" +
                    "</div>";
                message.setContent(htmlContent, "text/html; charset=utf-8");
            }
            
            Transport.send(message);
            
            String successMessage = purpose.equals("Email Verification") ? 
                "Verification code has been sent to your email." : 
                "Password reset code has been sent to your email.";
                
            JOptionPane.showMessageDialog(this, successMessage, "Code Sent", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (MessagingException ex) {
            ex.printStackTrace();
            String errorDetails = "";
            if (ex instanceof AuthenticationFailedException) {
                errorDetails = "Email authentication failed. Please check your email credentials.";
            } else if (ex.getMessage().contains("Invalid Addresses")) {
                errorDetails = "Invalid email address format.";
            } else {
                errorDetails = ex.getMessage();
            }
            showError("Failed to send email: " + errorDetails);
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Unexpected error occurred while sending email: " + ex.getMessage());
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        toggleThemeButton.setText(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode");
        applyTheme();
    }

    private void applyTheme() {
        Color bg = isDarkMode ? DARK_BG : Color.WHITE;
        Color fg = isDarkMode ? Color.WHITE : Color.BLACK;
        Color fieldBg = isDarkMode ? DARK_FIELD_BG : new Color(245, 245, 245);

        topPanel.setBackground(bg);
        toggleThemeButton.setBackground(isDarkMode ? new Color(70, 70, 70) : PRIMARY_COLOR);
        tabbedPane.setBackground(bg);
        tabbedPane.setForeground(fg);

        applyThemeToComponents(loginPanel, bg, fg, fieldBg);
        applyThemeToComponents(signupPanel, bg, fg, fieldBg);
        
        // Apply theme to the tabbed pane
        UIManager.put("TabbedPane.selected", isDarkMode ? new Color(50, 50, 50) : new Color(220, 225, 255));
        UIManager.put("TabbedPane.background", bg);
        UIManager.put("TabbedPane.foreground", fg);
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void applyThemeToComponents(Component c, Color bg, Color fg, Color fieldBg) {
        if (c instanceof JPanel) c.setBackground(bg);
        if (c instanceof JLabel) {
            c.setForeground(fg);
            // Don't override the color for special labels like app name
            if (c instanceof JLabel && ((JLabel) c).getText() != null && 
                (((JLabel) c).getText().equals("TaskMaster") || ((JLabel) c).getText().equals("Create an Account"))) {
                c.setForeground(PRIMARY_COLOR);
            }
            if (c instanceof JLabel && ((JLabel) c).getText() != null && 
                ((JLabel) c).getText().equals("Forgot Password?")) {
                c.setForeground(PRIMARY_COLOR);
            }
        }
        if (c instanceof JTextComponent) {
            if (!(c instanceof JTextArea)) {
                c.setBackground(fieldBg);
                c.setForeground(fg);
            }
        }
        if (c instanceof JButton && !((JButton) c).getText().equals("LOGIN") && 
            !((JButton) c).getText().equals("REGISTER")) {
            c.setBackground(isDarkMode ? new Color(70, 70, 70) : UIManager.getColor("Button.background"));
            c.setForeground(fg);
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                applyThemeToComponents(child, bg, fg, fieldBg);
            }
        }
    }
    
    private JTextField createStyledField(String placeholder) {
        JTextField field = new JTextField();
        setupFieldStyle(field, placeholder);
        return field;
    }
    
    private void setupFieldStyle(JTextComponent field, String placeholder) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, FIELD_HEIGHT));
        
        // Set placeholder text
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                    if (field instanceof JPasswordField) {
                        ((JPasswordField) field).setEchoChar('•');
                    }
                }
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                    if (field instanceof JPasswordField) {
                        ((JPasswordField) field).setEchoChar((char)0);
                    }
                }
            }
        });
        
        // For password fields, set echo char to 0 initially to show placeholder
        if (field instanceof JPasswordField) {
            ((JPasswordField) field).setEchoChar((char)0);
        }
    }
    
    private JButton createButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 36));
        return button;
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new Home()); } }