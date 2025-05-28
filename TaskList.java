import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Timer;
import java.util.TimerTask;
import com.toedter.calendar.JDateChooser;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import com.mongodb.client.*;
import org.bson.Document;
import static com.mongodb.client.model.Sorts.ascending;
import org.bson.conversions.Bson;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

public class TaskList extends JFrame {
    private String username;
    private DefaultTableModel tableModel;
    private JTable taskTable;
    private JTextField taskField;
    private JButton addButton, completeButton, deleteButton, updateButton, logoutButton, themeToggleButton;
    private JComboBox<String> filterDropdown, priorityDropdown, categoryDropdown;
    private JDateChooser dueDateChooser;
    private JSpinner timeSpinner;
    private boolean darkMode = false;
    private List<String> categories = new ArrayList<>(Arrays.asList("Work", "Personal", "Study", "Health"));

    public TaskList(String username) {
        this.username = username;
        setTitle("Task Manager - " + username);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setupUI();
        loadTasks("All");
        startRealTimeAlertCheck();
        setVisible(true);
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        Font font = new Font("Segoe UI", Font.PLAIN, 14);

        // Top Panel (Input + Filter)
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Task Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // Row 1: Task + Due Date + Time
        JPanel row1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        taskField = new JTextField(20);
        taskField.setFont(font);
        taskField.setToolTipText("Enter task description");

        dueDateChooser = new JDateChooser();
        dueDateChooser.setDateFormatString("dd-MM-yyyy");
        dueDateChooser.setToolTipText("Select due date");

        timeSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "hh:mm a");
        timeSpinner.setEditor(timeEditor);
        timeSpinner.setToolTipText("Set due time");

        row1Panel.add(new JLabel("Task:"));
        row1Panel.add(taskField);
        row1Panel.add(new JLabel("Due Date:"));
        row1Panel.add(dueDateChooser);
        row1Panel.add(new JLabel("Time:"));
        row1Panel.add(timeSpinner);

        // Row 2: Priority + Category + Add Button
        JPanel row2Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        priorityDropdown = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        priorityDropdown.setToolTipText("Set task priority");

        categoryDropdown = new JComboBox<>(categories.toArray(new String[0]));
        categoryDropdown.setEditable(true);
        categoryDropdown.setToolTipText("Select or add a category");

        addButton = createStyledButton("➕ Add Task");
        addButton.setToolTipText("Add a new task");

        row2Panel.add(new JLabel("Priority:"));
        row2Panel.add(priorityDropdown);
        row2Panel.add(new JLabel("Category:"));
        row2Panel.add(categoryDropdown);
        row2Panel.add(addButton);

        inputPanel.add(row1Panel);
        inputPanel.add(row2Panel);

        // Right Panel (Theme + Filter)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        themeToggleButton = createStyledButton(darkMode ? "☀️ Light Mode" : "🌙 Dark Mode");
        filterDropdown = new JComboBox<>(new String[]{"All", "Pending", "Completed", "Overdue"});
        filterDropdown.setToolTipText("Filter tasks by status");

        rightPanel.add(new JLabel("Filter:"));
        rightPanel.add(filterDropdown);
        rightPanel.add(themeToggleButton);

        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(rightPanel, BorderLayout.EAST);

        // Table Setup
        tableModel = new DefaultTableModel(new Object[]{"Task", "Priority", "Category", "Status", "Due Date"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Disable editing cells directly
            }
        };
        taskTable = new JTable(tableModel);
        taskTable.setAutoCreateRowSorter(true);
        taskTable.setFont(font);
        taskTable.setRowHeight(28);
        taskTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        JScrollPane scrollPane = new JScrollPane(taskTable);

        // Bottom Panel (Actions)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        completeButton = createStyledButton("✅ Complete");
        deleteButton = createStyledButton("🗑️ Delete");
        updateButton = createStyledButton("✏️ Edit");
        logoutButton = createStyledButton("🔒 Logout");

        bottomPanel.add(completeButton);
        bottomPanel.add(deleteButton);
        bottomPanel.add(updateButton);
        bottomPanel.add(logoutButton);

        // Add components to frame
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Event Listeners
        addButton.addActionListener(e -> addTask());
        completeButton.addActionListener(e -> markAsCompleted());
        deleteButton.addActionListener(e -> deleteTask());
        updateButton.addActionListener(e -> updateTask());
        logoutButton.addActionListener(e -> logout());
        themeToggleButton.addActionListener(e -> toggleTheme());
        filterDropdown.addActionListener(e -> loadTasks(filterDropdown.getSelectedItem().toString()));

        // Double-click to view task details
        taskTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showTaskDetails();
                }
            }
        });

        applyTheme();
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setFocusPainted(false);
        button.setBackground(new Color(59, 89, 182));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return button;
    }

    private void applyTheme() {
        Color bgColor = darkMode ? new Color(34, 34, 34) : Color.WHITE;
        Color fgColor = darkMode ? Color.WHITE : Color.BLACK;
        Color panelBg = darkMode ? new Color(45, 45, 45) : new JPanel().getBackground();

        getContentPane().setBackground(bgColor);
        taskField.setBackground(bgColor);
        taskField.setForeground(fgColor);
        taskTable.setBackground(bgColor);
        taskTable.setForeground(fgColor);
        taskTable.setGridColor(darkMode ? Color.DARK_GRAY : Color.LIGHT_GRAY);
        taskTable.getTableHeader().setBackground(darkMode ? new Color(60, 60, 60) : Color.LIGHT_GRAY);
        taskTable.getTableHeader().setForeground(fgColor);
        filterDropdown.setBackground(bgColor);
        filterDropdown.setForeground(fgColor);
        priorityDropdown.setBackground(bgColor);
        priorityDropdown.setForeground(fgColor);
        categoryDropdown.setBackground(bgColor);
        categoryDropdown.setForeground(fgColor);
        dueDateChooser.getCalendarButton().setBackground(bgColor);
        dueDateChooser.getCalendarButton().setForeground(fgColor);
        ((JSpinner.DefaultEditor) timeSpinner.getEditor()).getTextField().setBackground(bgColor);
        ((JSpinner.DefaultEditor) timeSpinner.getEditor()).getTextField().setForeground(fgColor);
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        themeToggleButton.setText(darkMode ? "☀️ Light Mode" : "🌙 Dark Mode");
        applyTheme();
    }

    private void loadTasks(String filter) {
        tableModel.setRowCount(0);
        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
            MongoCollection<Document> tasks = db.getCollection("tasks");

            Bson filterQuery = eq("username", username);
            switch (filter) {
                case "Pending":
                    filterQuery = and(filterQuery, eq("status", "Pending"));
                    break;
                case "Completed":
                    filterQuery = and(filterQuery, eq("status", "Completed"));
                    break;
                case "Overdue":
                    filterQuery = and(filterQuery, 
                        eq("status", "Pending"),
                        lt("dueDate", new Date()));
                    break;
            }

            FindIterable<Document> results = tasks.find(filterQuery).sort(ascending("dueDate"));
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a");

            for (Document doc : results) {
                String task = doc.getString("task");
                String priority = doc.getString("priority");
                String category = doc.getString("category");
                String status = doc.getString("status");
                Date dueDate = doc.getDate("dueDate");
                String dueDateStr = (dueDate != null) ? sdf.format(dueDate) : "N/A";

                tableModel.addRow(new Object[]{task, priority, category, status, dueDateStr});
            }
        } catch (Exception ex) {
            showError("Error loading tasks: " + ex.getMessage());
        }
    }

    private void addTask() {
        String task = taskField.getText().trim();
        Date date = dueDateChooser.getDate();
        Date time = (Date) timeSpinner.getValue();
        String priority = (String) priorityDropdown.getSelectedItem();
        String category = ((String) categoryDropdown.getSelectedItem()).trim();

        if (task.isEmpty()) {
            showError("Task description cannot be empty!");
            return;
        }
        if (date == null) {
            showError("Please select a due date!");
            return;
        }

        // Combine date and time
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(time);
        cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
        Date dueDateTime = cal.getTime();

        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
            MongoCollection<Document> tasks = db.getCollection("tasks");

            Document newTask = new Document("task", task)
                    .append("priority", priority)
                    .append("category", category)
                    .append("status", "Pending")
                    .append("username", username)
                    .append("createdAt", new Date())
                    .append("dueDate", dueDateTime);

            tasks.insertOne(newTask);
            taskField.setText("");
            loadTasks(filterDropdown.getSelectedItem().toString());
            JOptionPane.showMessageDialog(this, "✅ Task added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError("Failed to add task: " + ex.getMessage());
        }
    }

    private void markAsCompleted() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a task to mark as completed!");
            return;
        }

        String task = (String) tableModel.getValueAt(selectedRow, 0);
        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
            MongoCollection<Document> tasks = db.getCollection("tasks");

            tasks.updateOne(
                and(eq("username", username), eq("task", task)),
                set("status", "Completed")
            );
            loadTasks(filterDropdown.getSelectedItem().toString());
        } catch (Exception ex) {
            showError("Failed to update task: " + ex.getMessage());
        }
    }

    private void deleteTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a task to delete!");
            return;
        }

        String task = (String) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this task?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
                MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
                MongoCollection<Document> tasks = db.getCollection("tasks");
                tasks.deleteOne(and(eq("username", username), eq("task", task)));
                loadTasks(filterDropdown.getSelectedItem().toString());
            } catch (Exception ex) {
                showError("Failed to delete task: " + ex.getMessage());
            }
        }
    }

    private void updateTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a task to edit!");
            return;
        }

        String oldTask = (String) tableModel.getValueAt(selectedRow, 0);
        String oldPriority = (String) tableModel.getValueAt(selectedRow, 1);
        String oldCategory = (String) tableModel.getValueAt(selectedRow, 2);

        // Create edit dialog
        JTextField taskField = new JTextField(oldTask, 20);
        JComboBox<String> priorityBox = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        priorityBox.setSelectedItem(oldPriority);
        JComboBox<String> categoryBox = new JComboBox<>(categories.toArray(new String[0]));
        categoryBox.setSelectedItem(oldCategory);
        categoryBox.setEditable(true);

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Task:"));
        panel.add(taskField);
        panel.add(new JLabel("Priority:"));
        panel.add(priorityBox);
        panel.add(new JLabel("Category:"));
        panel.add(categoryBox);

        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Edit Task",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String newTask = taskField.getText().trim();
            String newPriority = (String) priorityBox.getSelectedItem();
            String newCategory = ((String) categoryBox.getSelectedItem()).trim();

            if (newTask.isEmpty()) {
                showError("Task description cannot be empty!");
                return;
            }

            try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
                MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
                MongoCollection<Document> tasks = db.getCollection("tasks");

                tasks.updateOne(
                    and(eq("username", username), eq("task", oldTask)),
                    combine(
                        set("task", newTask),
                        set("priority", newPriority),
                        set("category", newCategory)
                    )
                );
                loadTasks(filterDropdown.getSelectedItem().toString());
            } catch (Exception ex) {
                showError("Failed to update task: " + ex.getMessage());
            }
        }
    }

    private void showTaskDetails() {
        int row = taskTable.getSelectedRow();
        if (row == -1) return;

        String task = (String) tableModel.getValueAt(row, 0);
        String priority = (String) tableModel.getValueAt(row, 1);
        String category = (String) tableModel.getValueAt(row, 2);
        String status = (String) tableModel.getValueAt(row, 3);
        String dueDate = (String) tableModel.getValueAt(row, 4);

        String details = String.format(
            "<html><b>Task:</b> %s<br>" +
            "<b>Priority:</b> %s<br>" +
            "<b>Category:</b> %s<br>" +
            "<b>Status:</b> %s<br>" +
            "<b>Due Date:</b> %s</html>",
            task, priority, category, status, dueDate
        );

        JOptionPane.showMessageDialog(
            this,
            details,
            "Task Details",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void startRealTimeAlertCheck() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkOverdueTasks();
            }
        }, 0, 60000); // Check every minute
    }

    private void checkOverdueTasks() {
        try (MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017")) {
            MongoDatabase db = mongoClient.getDatabase("ToDoListAppDB");
            MongoCollection<Document> tasks = db.getCollection("tasks");

            FindIterable<Document> overdueTasks = tasks.find(
                and(
                    eq("username", username),
                    eq("status", "Pending"),
                    lt("dueDate", new Date())
                )
            );

            for (Document task : overdueTasks) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        TaskList.this,
                        "⚠️ Overdue Task: " + task.getString("task"),
                        "Task Alert",
                        JOptionPane.WARNING_MESSAGE
                    );
                });
            }
        } catch (Exception ex) {
            System.err.println("Error checking overdue tasks: " + ex.getMessage());
        }
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new Home().setVisible(true);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TaskList("testuser"));
    }
}