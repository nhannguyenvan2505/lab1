package calculator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.border.*;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import javax.swing.Timer;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;

public class Calculator extends JFrame {

    // Các biến thành viên để quản lý giao diện và trạng thái máy tính
    private JTextField displayField;
    private JLabel previousOperationLabel;
    private JLabel resultLabel;
    private JLabel errorLabel;
    private Font displayFont = new Font("Segoe UI", Font.BOLD, 24);
    private Font labelFont = new Font("Segoe UI", Font.PLAIN, 16);
    private Font historyFont = new Font("Monospaced", Font.PLAIN, 14);
    private Color displayTextColor = Color.BLACK;
    private boolean isDarkMode = false; // Chế độ giao diện (Light Mode mặc định)
    private JPanel mainPanel;
    private JPanel historyPanel; // Tham chiếu đến bảng lịch sử
    private long lastMinusButtonEventTime = 0;
    private Color backgroundColor;
    private Color buttonColor;
    private Color operatorColor;
    private Color equalsColor;
    private Color textColor;
    private Color displayBgColor;
    private DefaultListModel<String> historyListModel;
    private int negativeCount = 0;
    private double currentNumber = 0;
    private double previousNumber = 0;
    private String currentOperation = "";
    private boolean isNewNumber = true;
    private long lastMinusEventTime = 0;
    private static final long EVENT_THRESHOLD_MS = 200;
    private ArrayList<String> history = new ArrayList<>();
    private String pendingOperation = "";
    private String firstNumber = "";

    private DecimalFormat df = new DecimalFormat("#,##0.######");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private static final String HISTORY_FILE = System.getProperty("user.home") + File.separator + "CalculatorData" + File.separator + "history.json";

    public Calculator() {
        createHistoryDirectory();
        setTitle("Máy Tính Bỏ Túi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Khởi tạo mainPanel với hiệu ứng gradient
        mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color endColor = isDarkMode ? new Color(30, 40, 60) : new Color(220, 220, 220);
                GradientPaint gp = new GradientPaint(0, 0, backgroundColor, w, h, endColor);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Tạo các panel con
        JPanel displayPanel = createDisplayPanel();
        historyPanel = createHistoryPanel();
        JPanel buttonPanel = createButtonPanel();

        // Thêm các panel vào mainPanel
        mainPanel.add(displayPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        // Thêm mainPanel và historyPanel vào contentPane
        add(mainPanel, BorderLayout.CENTER);
        add(historyPanel, BorderLayout.EAST);

        // Đặt viền cho historyPanel
        historyPanel.setBorder(BorderFactory.createCompoundBorder(
                new ShadowBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Cập nhật giao diện sau khi thêm các thành phần
        updateTheme();
        getContentPane().setBackground(backgroundColor);

        // Đặt viền cho root pane
        getRootPane().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 50), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Thêm listener để xóa lịch sử khi đóng ứng dụng
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                clearHistoryOnExit();
            }
        });

        // Tải lịch sử từ file
        loadHistoryFromFile();

        // THÊM MỚI: Đảm bảo con trỏ hoạt động
        displayField.requestFocusInWindow();
        displayField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                displayField.requestFocusInWindow();
            }
        });
        // Thiết lập phím tắt
        setupKeyBindings();

        pack();
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    private JPanel createDisplayPanel() {
        // Tạo panel hiển thị kết quả và lịch sử phép tính
        JPanel displayPanel = new JPanel(new BorderLayout(5, 5));
        displayPanel.setBackground(backgroundColor);
        displayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tạo settingsPanel và thêm vào phía trên của displayPanel
        JPanel settingsPanel = createSettingsPanel();
        displayPanel.add(settingsPanel, BorderLayout.NORTH);

        previousOperationLabel = new JLabel("");
        previousOperationLabel.setFont(labelFont);
        previousOperationLabel.setForeground(textColor);
        previousOperationLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        displayField = new JTextField("");
        displayField.setFont(displayFont);
        displayField.setHorizontalAlignment(JTextField.RIGHT);
        displayField.setEditable(true); // Giữ editable để hỗ trợ con trỏ
        displayField.setBackground(displayBgColor);
        displayField.setForeground(displayTextColor);
        displayField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180), 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        resultLabel = new JLabel("");
        resultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        resultLabel.setForeground(textColor);
        resultLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        errorLabel = new JLabel("");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        errorLabel.setForeground(isDarkMode ? new Color(255, 80, 80) : new Color(200, 0, 0));
        errorLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        errorLabel.setVisible(false);

        JPanel labelPanel = new JPanel(new GridLayout(3, 1, 0, 5));
        labelPanel.setBackground(backgroundColor);
        labelPanel.add(previousOperationLabel);
        labelPanel.add(resultLabel);
        labelPanel.add(errorLabel);

        displayPanel.add(displayField, BorderLayout.CENTER);
        displayPanel.add(labelPanel, BorderLayout.SOUTH);

        return displayPanel;
    }

    private JPanel createHistoryPanel() {
        // Tạo panel hiển thị lịch sử phép tính
        JPanel panel = new JPanel(new BorderLayout(5, 5)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color startColor = isDarkMode ? new Color(20, 20, 30) : new Color(240, 240, 240);
                Color endColor = isDarkMode ? new Color(40, 40, 50) : new Color(200, 200, 200);
                GradientPaint gp = new GradientPaint(0, 0, startColor, w, h, endColor);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setPreferredSize(new Dimension(300, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tạo phần tiêu đề của bảng lịch sử
        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.setOpaque(false);

        // Nhãn "Lịch sử"
        JLabel historyLabel = new JLabel("Lịch sử");
        historyLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        historyLabel.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);

        // Tạo thanh tìm kiếm
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setOpaque(false);
        JTextField searchField = new JTextField();
        displayField.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        displayField.setHorizontalAlignment(JTextField.RIGHT);

// Ngăn JTextField chèn ký tự '-' khi bạn đã xử lý qua inputMap
        displayField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_MINUS) {
                    e.consume();
                }
            }
        });
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180), 1, true));
        searchField.setToolTipText("Nhập để tìm kiếm lịch sử...");
        searchField.setBackground(isDarkMode ? new Color(40, 40, 50) : new Color(230, 230, 230));
        searchField.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);

        JButton clearSearchButton = new JButton("X");
        clearSearchButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearSearchButton.setBackground(isDarkMode ? new Color(180, 40, 40) : new Color(220, 80, 80));
        clearSearchButton.setForeground(Color.WHITE);
        clearSearchButton.setFocusPainted(false);
        clearSearchButton.setBorder(BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180), 1, true));
        clearSearchButton.setPreferredSize(new Dimension(30, 20));
        clearSearchButton.setToolTipText("Xóa bộ lọc tìm kiếm");

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(clearSearchButton, BorderLayout.EAST);

        // Tạo panel chứa các nút "Xóa lịch sử" và "Xóa mục"
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        JButton clearButton = new JButton("Xóa lịch sử");
        styleButton(clearButton);
        clearButton.setToolTipText("Xóa toàn bộ lịch sử");

        JButton deleteSelectedButton = new JButton("Xóa mục");
        styleButton(deleteSelectedButton);
        deleteSelectedButton.setToolTipText("Xóa mục được chọn");

        buttonPanel.add(clearButton);
        buttonPanel.add(deleteSelectedButton);

        // Sử dụng BorderLayout để tránh nhãn "Lịch sử" bị đè bởi các nút
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        historyLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0)); // Thêm khoảng đệm bên trái cho nhãn "Lịch sử"
        titlePanel.add(historyLabel, BorderLayout.WEST); // Đặt nhãn "Lịch sử" ở phía Tây
        titlePanel.add(buttonPanel, BorderLayout.EAST); // Đặt các nút ở phía Đông

        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(searchPanel, BorderLayout.SOUTH);

        // Tạo danh sách lịch sử
        historyListModel = new DefaultListModel<>();
        JList<String> historyList = new JList<>(historyListModel);
        historyList.setFont(historyFont);
        historyList.setBackground(isDarkMode ? new Color(40, 40, 50) : new Color(230, 230, 230));
        historyList.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setFixedCellHeight(25);
        DefaultListCellRenderer renderer = (DefaultListCellRenderer) historyList.getCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);

        JScrollPane scrollPane = new JScrollPane(historyList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Thêm sự kiện tìm kiếm
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterHistory(searchField.getText());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterHistory(searchField.getText());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterHistory(searchField.getText());
            }
        });

        // Xóa bộ lọc tìm kiếm
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            updateHistoryDisplay();
        });

        // Xóa toàn bộ lịch sử
        clearButton.addActionListener(e -> {
            clearHistory();
        });

        // Xóa mục được chọn trong lịch sử
        deleteSelectedButton.addActionListener(e -> {
            int selectedIndex = historyList.getSelectedIndex();
            if (selectedIndex != -1) {
                int historyIndex = selectedIndex / 2;
                if (selectedIndex % 2 == 0) {
                    history.remove(historyIndex);
                    historyListModel.remove(selectedIndex + 1);
                    historyListModel.remove(selectedIndex);
                } else {
                    history.remove(historyIndex);
                    historyListModel.remove(selectedIndex);
                    historyListModel.remove(selectedIndex - 1);
                }
                saveHistoryToFile();
            } else {
                showError("Vui lòng chọn một mục để xóa!");
            }
        });

        return panel;
    }

    private JPanel createButtonPanel() {
        // GHI CHÚ: Panel chính chứa các nút, chia thành panel chức năng (AC, CE, √,...) và panel số/toán tử
        JPanel mainButtonPanel = new JPanel(new BorderLayout(5, 5));
        mainButtonPanel.setBackground(backgroundColor);

        // Tăng số cột để chứa thêm nút Deg→Rad và Rad→Deg
        JPanel functionPanel = new JPanel(new GridLayout(2, 7, 5, 5)); // Thay đổi từ 5 cột thành 7 cột
        functionPanel.setBackground(backgroundColor);
        addButton(functionPanel, "AC", e -> clearAll());
        addButton(functionPanel, "CE", e -> clearEntry());
        addButton(functionPanel, "←", e -> delete());
        addButton(functionPanel, "→", e -> forwardDelete());
        addButton(functionPanel, "√", e -> unaryOperation("√"));
        addButton(functionPanel, "x²", e -> unaryOperation("x²"));
        addButton(functionPanel, "n!", e -> unaryOperation("n!"));
        addButton(functionPanel, "log₁₀", e -> unaryOperation("log₁₀"));
        addButton(functionPanel, "ln", e -> unaryOperation("ln"));
        addButton(functionPanel, "sin", e -> unaryOperation("sin"));
        addButton(functionPanel, "cos", e -> unaryOperation("cos"));
        addButton(functionPanel, "tan", e -> unaryOperation("tan"));
        addButton(functionPanel, "cot", e -> unaryOperation("cot"));
        // GHI CHÚ: Thêm nút chuyển đổi Độ sang Radian và Radian sang Độ
        addButton(functionPanel, "Deg→Rad", e -> unaryOperation("Deg→Rad"));
        addButton(functionPanel, "Rad→Deg", e -> unaryOperation("Rad→Deg"));
        addButton(functionPanel, "Copy", e -> {
            // GHI CHÚ: Sao chép nội dung trường hiển thị vào clipboard
            StringSelection stringSelection = new StringSelection(displayField.getText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });

        JPanel numberPanel = new JPanel(new GridLayout(5, 4, 5, 5));
        numberPanel.setBackground(backgroundColor);

        addButton(numberPanel, "7", e -> appendNumber("7"));
        addButton(numberPanel, "8", e -> appendNumber("8"));
        addButton(numberPanel, "9", e -> appendNumber("9"));
        addButton(numberPanel, "÷", e -> setOperation("÷"));

        addButton(numberPanel, "4", e -> appendNumber("4"));
        addButton(numberPanel, "5", e -> appendNumber("5"));
        addButton(numberPanel, "6", e -> appendNumber("6"));
        addButton(numberPanel, "×", e -> setOperation("×"));

        addButton(numberPanel, "1", e -> appendNumber("1"));
        addButton(numberPanel, "2", e -> appendNumber("2"));
        addButton(numberPanel, "3", e -> appendNumber("3"));
        addButton(numberPanel, "-", e -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMinusButtonEventTime < EVENT_THRESHOLD_MS) {
                return; // Bỏ qua nếu sự kiện chuột quá gần nhau
            }
            lastMinusButtonEventTime = currentTime;

            String currentText = displayField.getText();
            int caretPosition = displayField.getCaretPosition();

            if (currentText.equals("-") && caretPosition == 1) {
                // Nếu đã có "-" và nhấn "-" lần nữa, triệt tiêu thành "0"
                displayField.setText("0");
                negativeCount = 0;
                isNewNumber = true;
                displayField.setCaretPosition(0);
                updateLiveResult();
                flashDisplay();
            } else if (currentText.startsWith("-") && caretPosition <= 1) {
                // Xóa dấu trừ để hiển thị số dương
                String newText = currentText.substring(1);
                displayField.setText(newText.isEmpty() ? "0" : newText);
                negativeCount = 0;
                isNewNumber = newText.isEmpty();
                displayField.setCaretPosition(0);
                updateLiveResult();
                flashDisplay();
            } else if (currentText.isEmpty() || currentText.equals("0") || (!pendingOperation.isEmpty() && pendingOperation.equals("√"))) {
                appendNumber("-");
                flashDisplay();
            } else if (isValidNumber(currentText)) {
                setOperation("-");
                flashDisplay();
            } else {
                showError("Vui lòng nhập số hợp lệ!");
                flashDisplay();
            }
        });

        addButton(numberPanel, "0", e -> appendNumber("0"));
        addButton(numberPanel, "%", e -> setOperation("%"));
        addButton(numberPanel, ".", e -> appendNumber("."));
        addButton(numberPanel, "+", e -> setOperation("+"));

        JButton equalButton = new JButton("=");
        styleButton(equalButton);
        equalButton.addActionListener(e -> equalButtonAction());

        JPanel equalPanel = new JPanel(new GridLayout(1, 1));
        equalPanel.setBackground(backgroundColor);
        equalPanel.add(equalButton);
        numberPanel.add(equalPanel);

        mainButtonPanel.add(functionPanel, BorderLayout.NORTH);
        mainButtonPanel.add(numberPanel, BorderLayout.CENTER);

        return mainButtonPanel;
    }

    private void addButton(JPanel panel, String text, ActionListener listener) {
        JButton button = new JButton(text);
        styleButton(button);
        button.addActionListener(listener);
        panel.add(button);
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180), 1, true));
        button.setContentAreaFilled(false);
        button.setOpaque(true);

        if (button.getText().matches("[0-9.]")) {
            button.setBackground(buttonColor);
            button.setForeground(isDarkMode ? new Color(220, 220, 220) : new Color(40, 40, 40));
        } else if (button.getText().equals("=")) {
            button.setBackground(equalsColor);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(operatorColor);
            button.setForeground(isDarkMode ? new Color(255, 80, 80) : new Color(0, 140, 255));
        }

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.getText().equals("=")) {
                    button.setBackground(isDarkMode ? new Color(0, 100, 180) : new Color(0, 100, 180));
                } else if (button.getText().matches("[0-9.]")) {
                    button.setBackground(buttonColor.brighter());
                } else {
                    button.setBackground(operatorColor.brighter());
                }
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.getText().equals("=")) {
                    button.setBackground(equalsColor);
                } else if (button.getText().matches("[0-9.]")) {
                    button.setBackground(buttonColor);
                } else {
                    button.setBackground(operatorColor);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(button.getBackground().darker());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.getText().equals("=")) {
                    button.setBackground(equalsColor);
                } else if (button.getText().matches("[0-9.]")) {
                    button.setBackground(buttonColor);
                } else {
                    button.setBackground(operatorColor);
                }
            }
        });
    }

    // Cập nhật appendNumber để hỗ trợ con trỏ
    private void appendNumber(String number) {
        System.out.println("Đang thêm số: " + number);
        String currentText = displayField.getText();
        int caretPosition = Math.min(displayField.getCaretPosition(), currentText.length());
        System.out.println("appendNumber: currentText='" + currentText + "', caretPosition=" + caretPosition + ", negativeCount=" + negativeCount);

        // Nếu không phải số, dấu chấm hoặc dấu trừ thì bỏ qua
        if (!"0123456789.-".contains(number)) {
            showError("Dữ liệu không hợp lệ!");
            flashDisplay();
            return;
        }

        StringBuilder newText = new StringBuilder(currentText);

        // Xử lý dấu trừ "-"
        if (number.equals("-")) {
            // Kiểm tra nếu currentText chỉ gồm toàn dấu '-'
            if (currentText.matches("-+")) {
                int dashCount = currentText.length();

                if (dashCount % 2 == 0) {
                    // Số lượng dấu trừ chẵn => thành phép cộng "+"
                    setOperation("+");
                } else {
                    // Số lượng dấu trừ lẻ => thành phép trừ "-"
                    setOperation("-");
                }
                flashDisplay();
                return;
            }

            if (currentText.equals("-")) {
                displayField.setText("0");
                displayField.setCaretPosition(0);
                negativeCount = 0;
                isNewNumber = true;
                updateLiveResult();
                flashDisplay();
                return;
            }

            if (currentText.startsWith("-") && caretPosition <= 1) {
                newText.deleteCharAt(0);
                String result = newText.toString();
                displayField.setText(result.isEmpty() ? "0" : result);
                displayField.setCaretPosition(0);
                negativeCount = 0;
                isNewNumber = result.isEmpty();
                updateLiveResult();
                flashDisplay();
                return;
            }

            if (currentText.isEmpty() || currentText.equals("0") || (!pendingOperation.isEmpty() && pendingOperation.equals("√"))) {
                if (currentText.equals("0")) {
                    newText.setLength(0); // Xoá "0"
                }
                int insertPosition = currentText.isEmpty() ? 0 : caretPosition;
                newText.insert(insertPosition, "-");
                String result = newText.toString();
                displayField.setText(result);
                displayField.setCaretPosition(insertPosition + 1);
                negativeCount = 1;
                isNewNumber = true;
                updateLiveResult();
                flashDisplay();
                return;
            }

            if (isValidNumber(currentText)) {
                setOperation("-");
                flashDisplay();
            } else {
                showError("Vui lòng nhập số hợp lệ!");
                flashDisplay();
            }
            return;
        }

        // Xử lý dấu chấm "."
        if (number.equals(".")) {
            if (currentText.contains(".")) {
                showError("Số đã có dấu chấm!");
                flashDisplay();
                return;
            }

            if (currentText.isEmpty() || currentText.equals("-")) {
                newText.append("0.");
                caretPosition = newText.length(); // cập nhật caret về cuối
            } else {
                newText.insert(caretPosition, ".");
                caretPosition++; // cập nhật caret đúng sau dấu chấm
            }

            String result = newText.toString();
            if (isValidNumber(result)) {
                displayField.setText(result);
                displayField.setCaretPosition(caretPosition);
                isNewNumber = false;
                updateLiveResult();
                flashDisplay();
            } else {
                showError("Dữ liệu không hợp lệ!");
                flashDisplay();
            }
            return;
        }

        // Thêm số bình thường
        newText.insert(caretPosition, number);
        String result = newText.toString();
        if (isValidNumber(result)) {
            displayField.setText(result);
            displayField.setCaretPosition(caretPosition + 1);
            isNewNumber = false;
            updateLiveResult();
            flashDisplay();
        } else {
            showError("Dữ liệu không hợp lệ!");
            flashDisplay();
        }
    }

    // THÊM MỚI: Phương thức clearEntry
    private void clearEntry() {
        displayField.setText("0");
        isNewNumber = true;
        negativeCount = 0;
        updateLiveResult();
        flashDisplay();
    }

    // THÊM MỚI: Phương thức delete (Backspace)
    private void delete() {
        int caretPosition = displayField.getCaretPosition();
        String currentText = displayField.getText();
        System.out.println("delete() called: caretPosition=" + caretPosition + ", currentText='" + currentText + "'");

        if (caretPosition > 0 && currentText.length() > 0) {
            StringBuilder newText = new StringBuilder(currentText);
            newText.deleteCharAt(caretPosition - 1);
            displayField.setText(newText.toString());
            displayField.setCaretPosition(caretPosition - 1);
            if (newText.length() == 0) {
                displayField.setText("0");
                isNewNumber = true;
            }
            updateLiveResult();
            flashDisplay();
        }
        displayField.requestFocusInWindow();
    }

    private void setOperation(String operation) {
        String currentText = displayField.getText();
        if (!currentText.isEmpty() && isValidNumber(currentText)) {
            if (!pendingOperation.isEmpty()) {
                equalButtonAction(); // Tính kết quả trước nếu có phép toán đang chờ
                currentText = displayField.getText();
            }
            firstNumber = currentText;
            currentOperation = operation;
            pendingOperation = "";
            displayField.setText("");
            isNewNumber = true;
            negativeCount = 0;
            updateLiveResult();
        } else if (operation.equals("-") && (currentText.isEmpty() || currentText.equals("0"))) {
            // Cho phép nhập số âm nếu chưa có gì hoặc đang là 0
            appendNumber("-");
        } else {
            showError("Vui lòng nhập số hợp lệ trước khi chọn toán tử!");
        }
    }

    // THÊM MỚI: Phương thức forwardDelete (Forward Delete)
    private void forwardDelete() {
        int caretPosition = displayField.getCaretPosition();
        String currentText = displayField.getText();

        if (caretPosition < currentText.length() && currentText.length() > 0) {
            StringBuilder newText = new StringBuilder(currentText);
            newText.deleteCharAt(caretPosition);
            displayField.setText(newText.toString());
            displayField.setCaretPosition(caretPosition);
            if (newText.length() == 0) {
                displayField.setText("0");
                isNewNumber = true;
            }
            updateLiveResult();
            flashDisplay();
        }
    }

    private void unaryOperation(String operation) {
        try {
            if (operation.equals("√") || operation.equals("log₁₀") || operation.equals("ln")) {
                pendingOperation = operation;
                previousOperationLabel.setText(operation);
                displayField.setText("");
                isNewNumber = true;
                negativeCount = 0;
                return;
            }

            String currentText = displayField.getText().replace(",", "");
            if (currentText.isEmpty() || currentText.equals("-")) {
                showError("Vui lòng nhập số!");
                return;
            }
            double number = Double.parseDouble(currentText);
            double result = 0;
            String expression = "";
            switch (operation) {
                case "x²":
                    result = Math.pow(number, 2);
                    expression = "(" + df.format(number) + ")²";
                    break;
                case "n!":
                    if (number < 0 || number != (int) number) {
                        showError("Giai thừa chỉ áp dụng cho số nguyên không âm!");
                        return;
                    }
                    result = factorial((int) number);
                    expression = "(" + (int) number + ")!";
                    break;
                case "sin":
                    result = sin(degreesToRadians(number));
                    expression = "sin(" + df.format(number) + "°)";
                    break;
                case "cos":
                    result = cos(degreesToRadians(number));
                    expression = "cos(" + df.format(number) + "°)";
                    break;
                case "tan":
                    result = tan(degreesToRadians(number));
                    expression = "tan(" + df.format(number) + "°)";
                    break;
                case "cot":
                    result = cot(degreesToRadians(number));
                    expression = "cot(" + df.format(number) + "°)";
                    break;
                // GHI CHÚ: Chuyển đổi từ Độ sang Radian
                case "Deg→Rad":
                    result = degreesToRadians(number);
                    expression = df.format(number) + "° → rad";
                    break;
                // GHI CHÚ: Chuyển đổi từ Radian sang Độ
                case "Rad→Deg":
                    result = radiansToDegrees(number);
                    expression = df.format(number) + " rad → °";
                    break;
            }
            if (validateResult(result)) {
                addToHistory(expression, result);
                if (result % 1 == 0) {
                    resultLabel.setText("= " + String.format("%,d", (long) result));
                } else {
                    resultLabel.setText("= " + df.format(result));
                }
                previousOperationLabel.setText("");
                currentOperation = "";
                pendingOperation = "";
                isNewNumber = true;
                negativeCount = 0;
            }
        } catch (NumberFormatException e) {
            showError("Dữ liệu không hợp lệ!");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private double factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Giai thừa không được định nghĩa cho số âm.");
        }
        if (n > 170) {
            throw new ArithmeticException("Số quá lớn! Giai thừa vượt quá giới hạn kiểu double.");
        }
        double result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    private double logBase10(double x) {
        return Math.log10(x);
    }

    private double naturalLog(double x) {
        return Math.log(x);
    }

    private double sin(double x) {
        return Math.sin(x);
    }

    private double cos(double x) {
        return Math.cos(x);
    }

    private double tan(double x) {
        return Math.tan(x);
    }

    private double cot(double x) {
        return 1.0 / Math.tan(x);
    }

    private double degreesToRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    private double radiansToDegrees(double radians) {
        return Math.toDegrees(radians);
    }

    private boolean validateResult(double result) {
        if (Double.isNaN(result)) {
            showError("Kết quả không xác định (NaN)!");
            return false;
        }
        if (Double.isInfinite(result)) {
            showError("Kết quả vô cực!");
            return false;
        }
        if (Math.abs(result) > 1e308) {
            showError("Kết quả quá lớn!");
            return false;
        }
        return true;
    }

    private double calculate(double num1, double num2, String operator) {
        double result = 0;
        switch (operator) {
            case "+":
                result = num1 + num2;
                break;
            case "-":
                result = num1 - num2;
                break;
            case "×":
                result = num1 * num2;
                break;
            case "÷":
                if (num2 == 0) {
                    showError("Không thể chia cho 0!");
                    throw new ArithmeticException("Divide by zero");
                }
                result = num1 / num2;
                break;
            case "%":
                result = num1 * num2 / 100;
                break;
            default:
                showError("Toán tử không hợp lệ!");
                throw new IllegalArgumentException("Invalid operator: " + operator);
        }
        return result;
    }

    private void updateLiveResult() {
        if (!currentOperation.isEmpty() && !firstNumber.isEmpty()) {
            try {
                String currentText = displayField.getText();
                if (currentText.isEmpty() || currentText.equals("-")) {
                    resultLabel.setText("");
                    return;
                }
                double currentNum = parseNumber(currentText);
                double num1 = parseNumber(firstNumber);
                double result = 0;

                switch (currentOperation) {
                    case "+":
                        result = num1 + currentNum;
                        break;
                    case "-":
                        result = num1 - currentNum;
                        break;
                    case "×":
                        result = num1 * currentNum;
                        break;
                    case "÷":
                        if (currentNum == 0) {
                            resultLabel.setText("");
                            return;
                        }
                        result = num1 / currentNum;
                        break;
                    case "%":
                        result = num1 * currentNum / 100;
                        break;
                }

                if (validateResult(result)) {
                    if (result % 1 == 0) {
                        resultLabel.setText("= " + String.format("%,d", (long) result));
                    } else {
                        resultLabel.setText("= " + df.format(result));
                    }
                } else {
                    resultLabel.setText("");
                }
            } catch (NumberFormatException e) {
                resultLabel.setText("");
            }
        } else {
            resultLabel.setText("");
        }
    }

    private void updateHistoryDisplay() {
        if (historyListModel == null) {
            System.err.println("historyListModel chưa được khởi tạo!");
            return;
        }

        historyListModel.clear();

        if (history == null || history.isEmpty()) {
            return;
        }

        for (int i = 0; i < history.size(); i++) {
            String entry = history.get(i);
            if (entry != null) {
                historyListModel.addElement(entry.trim());
                if (i < history.size() - 1) {
                    historyListModel.addElement("----");
                }
            }
        }
    }

    private void addToHistory(String expression, double result) {
        String timeStamp = timeFormat.format(new Date());
        String historyEntry = timeStamp + " | " + expression + " = " + df.format(result);
        history.add(historyEntry);
        updateHistoryDisplay();
        saveHistoryToFile();
    }

    private void clearHistory() {
        history.clear();
        if (historyListModel != null) {
            historyListModel.clear();
        }
        try (FileWriter fw = new FileWriter(HISTORY_FILE)) {
            fw.write("");
            System.out.println("Đã xóa lịch sử trong file: " + HISTORY_FILE);
        } catch (IOException e) {
            System.err.println("Lỗi khi xóa file: " + e.getMessage());
            showError("Không thể xóa lịch sử: " + e.getMessage());
        }
    }

    private void clearAll() {
        // Sửa đổi để xóa một ký tự (backspace) thay vì xóa toàn bộ
        int caretPosition = displayField.getCaretPosition(); // Lấy vị trí con trỏ
        String currentText = displayField.getText(); // Lấy văn bản hiện tại trên màn hình
        System.out.println("clearAll() called: caretPosition=" + caretPosition + ", currentText='" + currentText + "'"); // Gỡ lỗi: In thông tin vị trí con trỏ và văn bản
        if (caretPosition > 0 && currentText.length() > 0) { // Kiểm tra xem có ký tự để xóa không
            StringBuilder newText = new StringBuilder(currentText); // Tạo bản sao văn bản để chỉnh sửa
            newText.deleteCharAt(caretPosition - 1); // Xóa ký tự ngay trước con trỏ
            displayField.setText(newText.toString()); // Cập nhật màn hình với văn bản mới
            displayField.setCaretPosition(caretPosition - 1); // Di chuyển con trỏ về bên trái một vị trí
            if (newText.length() == 0) { // Nếu văn bản rỗng sau khi xóa
                displayField.setText("0"); // Đặt màn hình về "0"
                isNewNumber = true; // Đánh dấu là số mới
            }
            updateLiveResult(); // Cập nhật kết quả trực tiếp
            flashDisplay(); // Tạo hiệu ứng nhấp nháy
        } else {
            System.out.println("Không xóa: caretPosition=" + caretPosition + ", textLength=" + currentText.length()); // Gỡ lỗi: Không thể xóa nếu không có ký tự
        }
        displayField.requestFocusInWindow(); // Đảm bảo màn hình giữ tiêu điểm
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);

        JDialog errorDialog = new JDialog(this, "Lỗi", true);
        errorDialog.setLayout(new BorderLayout(10, 10));
        errorDialog.setSize(300, 150);
        errorDialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, isDarkMode ? new Color(255, 80, 80) : new Color(200, 0, 0),
                        w, h, isDarkMode ? new Color(200, 50, 50) : new Color(150, 0, 0));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel messageLabel = new JLabel(message);
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton closeButton = new JButton("Đóng");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeButton.setBackground(new Color(255, 255, 255, 230));
        closeButton.setForeground(new Color(255, 80, 80));
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(true);
        closeButton.addActionListener(e -> {
            errorDialog.dispose();
            errorLabel.setVisible(false);
        });

        closeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setBackground(new Color(255, 255, 255, 255));
                closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setBackground(new Color(255, 255, 255, 230));
            }
        });

        mainPanel.add(messageLabel, BorderLayout.CENTER);
        mainPanel.add(closeButton, BorderLayout.SOUTH);

        errorDialog.add(mainPanel);

        javax.swing.Timer timer = new javax.swing.Timer(3000, e -> {
            errorDialog.dispose();
            errorLabel.setVisible(false);
        });
        timer.setRepeats(false);
        timer.start();

        errorDialog.setVisible(true);
    }

    private class ShadowBorder extends AbstractBorder {

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int shadow = 3;
            for (int i = 0; i < shadow; i++) {
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.drawRoundRect(x + i, y + i, width - i * 2 - 1, height - i * 2 - 1, 10, 10);
            }
            g2d.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 4, 4, 4);
        }
    }

    private void clearHistoryOnExit() {
        System.out.println("\n=== Đóng ứng dụng ===");
        System.out.println("Số phép tính trong RAM: " + history.size());
        if (history.size() > 0) {
            System.out.println("Danh sách phép tính hiện tại:");
            for (String entry : history) {
                System.out.println("- " + entry.trim());
            }
        }
        // Không xóa history hoặc file, để giữ lại lịch sử
        System.out.println("Lịch sử đã được giữ lại.");
    }

    private void createHistoryDirectory() {
        File directory = new File(System.getProperty("user.home") + File.separator + "CalculatorData");
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void filterHistory(String keyword) {
        if (historyListModel == null) {
            System.err.println("historyListModel chưa được khởi tạo!");
            return;
        }

        historyListModel.clear();
        if (keyword == null || keyword.trim().isEmpty()) {
            for (int i = 0; i < history.size(); i++) {
                String entry = history.get(i);
                if (entry != null) {
                    historyListModel.addElement(entry.trim());
                    if (i < history.size() - 1) {
                        historyListModel.addElement("----");
                    }
                }
            }
            return;
        }

        String lowerCaseKeyword = keyword.toLowerCase();
        for (int i = 0; i < history.size(); i++) {
            String entry = history.get(i);
            if (entry != null && entry.toLowerCase().contains(lowerCaseKeyword)) {
                historyListModel.addElement(entry.trim());
                if (i < history.size() - 1 && history.subList(i + 1, history.size()).stream()
                        .anyMatch(e -> e != null && e.toLowerCase().contains(lowerCaseKeyword))) {
                    historyListModel.addElement("----");
                }
            }
        }
    }

    private void saveHistoryToFile() {
        createHistoryDirectory();
        JSONArray jsonArray = new JSONArray();
        for (String entry : history) {
            if (entry != null && !entry.trim().isEmpty()) {
                JSONObject jsonEntry = new JSONObject();
                jsonEntry.put("entry", entry.trim());
                jsonArray.put(jsonEntry);
            }
        }
        try (FileWriter file = new FileWriter(HISTORY_FILE)) {
            file.write(jsonArray.toString(2));
            file.flush(); // Đảm bảo dữ liệu được ghi ngay lập tức
            System.out.println("Đã lưu " + jsonArray.length() + " phép tính vào file: " + HISTORY_FILE);
            System.out.println("Nội dung vừa ghi vào file: " + jsonArray.toString(2));
            // Kiểm tra nội dung file sau khi ghi
            try (FileReader reader = new FileReader(HISTORY_FILE)) {
                StringBuilder content = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    content.append((char) c);
                }
                System.out.println("Nội dung thực tế trong file sau khi ghi: " + content.toString());
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi lưu file: " + e.getMessage());
            showError("Không thể lưu lịch sử vào file: " + e.getMessage());
        }
    }

    private void loadHistoryFromFile() {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            System.out.println("Tệp lịch sử không tồn tại: " + HISTORY_FILE);
            return;
        }
        if (!file.canRead()) {
            System.err.println("Không có quyền đọc file: " + HISTORY_FILE);
            showError("Không có quyền đọc file lịch sử!");
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            StringBuilder content = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                content.append((char) c);
            }
            String contentStr = content.toString().trim();
            if (contentStr.isEmpty()) {
                System.out.println("Tệp lịch sử trống: " + HISTORY_FILE);
                return;
            }
            // Kiểm tra xem nội dung có bắt đầu bằng '[' (mảng JSON) không
            if (!contentStr.startsWith("[")) {
                System.err.println("Nội dung file không phải mảng JSON: " + contentStr);
                showError("File lịch sử không đúng định dạng JSON!");
                return;
            }

            history.clear();
            JSONArray jsonArray = new JSONArray(contentStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonEntry = jsonArray.getJSONObject(i);
                if (jsonEntry.has("entry")) {
                    String entry = jsonEntry.getString("entry").replace("\n", " | ");
                    history.add(entry);
                } else {
                    System.err.println("Phần tử JSON thiếu trường 'entry' tại vị trí " + i);
                }
            }
            updateHistoryDisplay();
            System.out.println("Đã đọc " + history.size() + " phép tính từ file: " + HISTORY_FILE);
        } catch (IOException e) {
            System.err.println("Lỗi khi đọc file: " + e.getMessage());
            showError("Không thể đọc lịch sử: " + e.getMessage());
        } catch (org.json.JSONException e) {
            System.err.println("Lỗi phân tích JSON: " + e.getMessage());
            showError("Lỗi định dạng file lịch sử không hợp lệ: " + e.getMessage());
        }
    }

    private void resetHistoryFile() {
        try (FileWriter file = new FileWriter(HISTORY_FILE)) {
            file.write("[]");
            System.out.println("Đã tạo lại file lịch sử trống: " + HISTORY_FILE);
        } catch (IOException e) {
            System.err.println("Lỗi khi tạo lại file lịch sử: " + e.getMessage());
            showError("Không thể tạo lại file lịch sử!");
        }
    }

    private void updateTheme() {
        // Cập nhật màu sắc dựa trên chế độ Dark Mode hoặc Light Mode
        if (isDarkMode) {
            backgroundColor = new Color(20, 20, 30);
            buttonColor = new Color(60, 60, 80, 230);
            operatorColor = new Color(80, 80, 100, 230);
            equalsColor = new Color(0, 120, 215);
            textColor = Color.WHITE;
            displayBgColor = new Color(40, 40, 50);
            displayTextColor = Color.WHITE;
        } else {
            backgroundColor = new Color(245, 245, 245);
            buttonColor = new Color(220, 220, 220, 230);
            operatorColor = new Color(200, 200, 200, 230);
            equalsColor = new Color(0, 120, 215);
            textColor = new Color(40, 40, 40);
            displayBgColor = new Color(255, 255, 255);
            displayTextColor = new Color(40, 40, 40);
        }

        // Cập nhật trường hiển thị
        if (displayField != null) {
            displayField.setBackground(displayBgColor);
            displayField.setForeground(displayTextColor);
            displayField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180), 1, true),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
        }

        // Cập nhật các nhãn
        if (previousOperationLabel != null) {
            previousOperationLabel.setForeground(textColor);
        }
        if (resultLabel != null) {
            resultLabel.setForeground(textColor);
        }
        if (errorLabel != null) {
            errorLabel.setForeground(isDarkMode ? new Color(255, 80, 80) : new Color(200, 0, 0));
        }

        // Cập nhật nền content pane
        if (getContentPane() != null) {
            getContentPane().setBackground(backgroundColor);
        }

        // Vẽ lại main panel
        if (mainPanel != null) {
            mainPanel.repaint();
        }

        // Cập nhật history panel nếu nó tồn tại
        if (historyPanel != null) {
            updateHistoryPanelTheme(historyPanel);
        }

        // Làm mới giao diện
        revalidate();
        repaint();
    }

    private void updateHistoryPanelTheme(JPanel panel) {
        // Cập nhật giao diện của history panel theo chế độ Dark Mode hoặc Light Mode
        panel.repaint();

        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel subPanel = (JPanel) comp;
                subPanel.setBackground(backgroundColor);
                subPanel.repaint();
                updateHistoryPanelTheme(subPanel);
            } else if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                label.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
            } else if (comp instanceof JTextField) {
                JTextField textField = (JTextField) comp;
                textField.setBackground(isDarkMode ? new Color(40, 40, 50) : new Color(230, 230, 230));
                textField.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                textField.setBorder(BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180), 1, true));
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                JList<?> list = (JList<?>) scrollPane.getViewport().getView();
                if (list != null) {
                    list.setBackground(isDarkMode ? new Color(40, 40, 50) : new Color(230, 230, 230));
                    list.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                }
            } else if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                styleButton(button);
            }
        }
    }

    private JPanel createSettingsPanel() {
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        settingsPanel.setBackground(backgroundColor);

        JButton themeButton = new JButton(isDarkMode ? "LightMode" : "DarkMode");
        themeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        styleButton(themeButton);
        themeButton.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            themeButton.setText(isDarkMode ? "LightMode" : "DarkMode");
            updateTheme();
        });

        JComboBox<String> fontComboBox = new JComboBox<>(new String[]{"Segoe UI", "Arial", "Times New Roman", "Courier New"});
        fontComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fontComboBox.setSelectedItem("Segoe UI");
        fontComboBox.setBackground(isDarkMode ? new Color(40, 40, 50) : new Color(230, 230, 230));
        fontComboBox.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
        fontComboBox.setBorder(BorderFactory.createLineBorder(isDarkMode ? new Color(80, 80, 80) : new Color(180, 180, 180), 1, true));
        fontComboBox.addActionListener(e -> {
            String selectedFont = (String) fontComboBox.getSelectedItem();
            displayFont = new Font(selectedFont, Font.BOLD, 24);
            labelFont = new Font(selectedFont, Font.PLAIN, 16);
            historyFont = new Font(selectedFont, Font.PLAIN, 14);
            displayField.setFont(displayFont);
            previousOperationLabel.setFont(labelFont);
            resultLabel.setFont(new Font(selectedFont, Font.PLAIN, 18));
            errorLabel.setFont(new Font(selectedFont, Font.PLAIN, 14));
            if (historyPanel != null) {
                updateHistoryPanelFont(historyPanel, historyFont);
            }
            SwingUtilities.updateComponentTreeUI(this);
            repaint();
        });

        JButton colorButton = new JButton("Chọn Màu Chữ");
        colorButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        styleButton(colorButton);
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Chọn Màu Chữ Hiển Thị", displayTextColor);
            if (newColor != null) {
                displayTextColor = newColor;
                displayField.setForeground(displayTextColor);
            }
        });

        JButton helpButton = new JButton("Trợ giúp");
        helpButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        styleButton(helpButton);
        helpButton.addActionListener(e -> showHelpDialog());

        settingsPanel.add(themeButton);
        settingsPanel.add(new JLabel("Phông Chữ:"));
        settingsPanel.add(fontComboBox);
        settingsPanel.add(colorButton);
        settingsPanel.add(helpButton);

        return settingsPanel;
    }

    private void updateHistoryPanelFont(JPanel panel, Font font) {
        // Cập nhật phông chữ cho các thành phần trong history panel
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                updateHistoryPanelFont((JPanel) comp, font);
            } else if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                label.setFont(new Font(font.getFamily(), Font.BOLD, 16));
            } else if (comp instanceof JTextField) {
                JTextField textField = (JTextField) comp;
                textField.setFont(new Font(font.getFamily(), Font.PLAIN, 14));
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                JList<?> list = (JList<?>) scrollPane.getViewport().getView();
                if (list != null) {
                    list.setFont(font);
                }
            } else if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setFont(new Font(font.getFamily(), Font.PLAIN, 12));
            }
        }
    }

    private void equalButtonAction() {
        try {
            String currentText = displayField.getText();
            if (currentText.isEmpty() || currentText.equals("-")) {
                showError("Vui lòng nhập số!");
                return;
            }
// Kiểm tra nếu đầu vào chứa nhiều toán tử hoặc dấu ngoặc

            if (!pendingOperation.isEmpty()) {
                double currentNum = parseNumber(currentText);
                double result = 0;
                String expression = "";

                switch (pendingOperation) {
                    case "√":
                        if (currentNum < 0) {
                            showError("Không thể tính căn bậc hai của số âm!");
                            return;
                        }
                        result = Math.sqrt(currentNum);
                        expression = "√(" + df.format(currentNum) + ")";
                        break;
                    case "log₁₀":
                        if (currentNum <= 0) {
                            showError("Không thể tính log₁₀ của số không dương!");
                            return;
                        }
                        result = logBase10(currentNum);
                        expression = "log₁₀(" + df.format(currentNum) + ")";
                        break;
                    case "ln":
                        if (currentNum <= 0) {
                            showError("Không thể tính ln của số không dương!");
                            return;
                        }
                        result = naturalLog(currentNum);
                        expression = "ln(" + df.format(currentNum) + ")";
                        break;
                    default:
                        showError("Toán tử không hợp lệ!");
                        return;
                }

                if (validateResult(result)) {
                    addToHistory(expression, result);
                    displayField.setText(formatResult(result));
                    previousOperationLabel.setText("");
                    resultLabel.setText("");
                    pendingOperation = "";
                    currentOperation = "";
                    isNewNumber = true;
                    negativeCount = result < 0 ? 1 : 0;
                }
                return;
            }

            if (!currentOperation.isEmpty() && !firstNumber.isEmpty()) {
                double num1 = parseNumber(firstNumber);
                double num2 = parseNumber(currentText);
                double result = calculate(num1, num2, currentOperation);

                if (validateResult(result)) {
                    String expression = df.format(num1) + currentOperation + (num2 < 0 ? "(" + df.format(num2) + ")" : df.format(num2));
                    addToHistory(expression, result);
                    displayField.setText(formatResult(result));
                    firstNumber = formatResult(result);
                    previousOperationLabel.setText("");
                    resultLabel.setText("");
                    currentOperation = "";
                    pendingOperation = "";
                    isNewNumber = true;
                    negativeCount = result < 0 ? 1 : 0;
                }
            } else {
                double currentNum = parseNumber(currentText);
                if (validateResult(currentNum)) {
                    String expression = df.format(currentNum);
                    addToHistory(expression, currentNum);
                    displayField.setText(formatResult(currentNum));
                    firstNumber = formatResult(currentNum);
                    previousOperationLabel.setText("");
                    resultLabel.setText("");
                    isNewNumber = true;
                    negativeCount = currentNum < 0 ? 1 : 0;
                }
            }
        } catch (NumberFormatException ex) {
            showError("Dữ liệu không hợp lệ!");
        } catch (ArithmeticException | IllegalArgumentException ex) {
            // Lỗi đã được hiển thị trong calculate()
        }
    }

// Thêm phương thức hỗ trợ định dạng kết quả
    private String formatResult(double result) {
        if (result % 1 == 0) {
            return String.format("%d", (long) result);
        } else {
            return df.format(result);
        }
    }

    private boolean isValidNumber(String token) {
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private double parseNumber(String text) throws NumberFormatException {
        String cleanText = text.replace(",", "");
        return Double.parseDouble(cleanText);
    }

    private void flashDisplay() {
        Color originalBg = displayField.getBackground();
        displayField.setBackground(new Color(200, 200, 255));
        Timer timer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayField.setBackground(originalBg);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void setupKeyBindings() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // Số (0-9 và bàn phím số)
        for (int i = 0; i <= 9; i++) {
            inputMap.put(KeyStroke.getKeyStroke(Character.forDigit(i, 10)), "digit" + i);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + i, 0), "digit" + i);
            final String digit = String.valueOf(i);
            actionMap.put("digit" + i, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    appendNumber(digit);
                    flashDisplay();
                }
            });
        }

        // THÊM MỚI: Phím tắt cho CE
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "clearEntry");
        actionMap.put("clearEntry", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearEntry();
                flashDisplay();
            }
        });

        // Toán tử: +
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), "plus");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), "plus");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.SHIFT_DOWN_MASK), "plus"); // Shift + = cho dấu +
        actionMap.put("plus", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setOperation("+");
                flashDisplay();
            }
        });

        // Toán tử: -
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "minus");
        actionMap.put("minus", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMinusEventTime < 300) { // Tăng lên 300ms để ngăn sự kiện lặp
                    return; // Bỏ qua nếu sự kiện quá gần nhau
                }
                lastMinusEventTime = currentTime;

                String currentText = displayField.getText();
                int caretPosition = displayField.getCaretPosition();
                // Đảm bảo caretPosition hợp lệ
                caretPosition = Math.min(caretPosition, currentText.length());
                System.out.println("Phím minus pressed: currentText='" + currentText + "', caretPosition=" + caretPosition + ", negativeCount=" + negativeCount);

                // Kiểm tra chuỗi currentText có phải là dấu "-" liên tiếp không
                if (currentText.matches("-+")) {
                    int dashCount = currentText.length();

                    if (dashCount % 2 == 0) {
                        // Số lượng dấu trừ chẵn => thành phép cộng "+"
                        setOperation("+");
                    } else {
                        // Số lượng dấu trừ lẻ => thành phép trừ "-"
                        setOperation("-");
                    }
                    flashDisplay();
                    return;
                }

                // Nếu chỉ có một dấu "-" hiện tại
                if (currentText.equals("-")) {
                    // Nếu đã có dấu "-", triệt tiêu thành "0"
                    displayField.setText("0");
                    negativeCount = 0;
                    isNewNumber = true;
                    displayField.setCaretPosition(0);
                    updateLiveResult();
                    flashDisplay();
                } else if (currentText.startsWith("-") && caretPosition <= 1) {
                    // Xóa dấu trừ để hiển thị số dương
                    String newText = currentText.substring(1);
                    displayField.setText(newText.isEmpty() ? "0" : newText);
                    negativeCount = 0;
                    isNewNumber = newText.isEmpty();
                    displayField.setCaretPosition(0);
                    updateLiveResult();
                    flashDisplay();
                } else if (currentText.isEmpty() || currentText.equals("0") || (!pendingOperation.isEmpty() && pendingOperation.equals("√"))) {
                    // Thêm dấu trừ cho số âm
                    appendNumber("-");
                    flashDisplay();
                } else if (isValidNumber(currentText)) {
                    // Nếu là số hợp lệ, thực hiện phép trừ
                    setOperation("-");
                    flashDisplay();
                } else {
                    showError("Vui lòng nhập số hợp lệ!");
                    flashDisplay();
                }
            }
        });

        // Hỗ trợ dấu chấm
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0), "period");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DECIMAL, 0), "period");
        actionMap.put("period", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appendNumber(".");
                flashDisplay();
            }
        });

        // Toán tử: *
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ASTERISK, 0), "multiply");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, 0), "multiply");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_8, InputEvent.SHIFT_DOWN_MASK), "multiply"); // Shift + 8 cho dấu *
        actionMap.put("multiply", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setOperation("×");
                flashDisplay();
            }
        });

        // Toán tử: /
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0), "divide");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, 0), "divide");
        actionMap.put("divide", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setOperation("÷");
                flashDisplay();
            }
        });

        // Toán tử: %
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_5, InputEvent.SHIFT_DOWN_MASK), "percent"); // Shift + 5 cho bàn phím US
        inputMap.put(KeyStroke.getKeyStroke("%"), "percent"); // Trực tiếp ký tự % cho bàn phím non-US
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), "percent"); // Ctrl + P làm phím dự phòng
        actionMap.put("percent", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setOperation("%");
                flashDisplay();
            }
        });

        // Dấu chấm thập phân
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0), "decimal");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DECIMAL, 0), "decimal");
        actionMap.put("decimal", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appendNumber(".");
                flashDisplay();
            }
        });

        // Phép toán đơn
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "sqrt");
        actionMap.put("sqrt", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("√");
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "square");
        actionMap.put("square", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("x²");
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "factorial");
        actionMap.put("factorial", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("n!");
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "log10");
        actionMap.put("log10", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("log₁₀");
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "ln");
        actionMap.put("ln", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("ln");
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "sin");
        actionMap.put("sin", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("sin");
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "cos");
        actionMap.put("cos", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("cos");
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "tan");
        actionMap.put("tan", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("tan");
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "cot");
        actionMap.put("cot", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("cot");
                flashDisplay();
            }
        });

        // GHI CHÚ: Phím tắt cho chuyển đổi Độ sang Radian
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "degToRad");
        actionMap.put("degToRad", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("Deg→Rad");
                flashDisplay();
            }
        });

        // GHI CHÚ: Phím tắt cho chuyển đổi Radian sang Độ
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "radToDeg");
        actionMap.put("radToDeg", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                unaryOperation("Rad→Deg");
                flashDisplay();
            }
        });

        // Hành động
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "equals");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "equals");
        actionMap.put("equals", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                equalButtonAction();
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "backspace");
        actionMap.put("backspace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Backspace pressed");
                delete();
                flashDisplay();
            }
        });

        // THÊM MỚI: Phím Delete và Right Arrow cho forwardDelete
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "forwardDelete");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "forwardDelete");
        actionMap.put("forwardDelete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                forwardDelete();
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "clear");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
        actionMap.put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAll();
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK), "clearHistory");
        actionMap.put("clearHistory", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearHistory();
                flashDisplay();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "toggleTheme");
        actionMap.put("toggleTheme", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                isDarkMode = !isDarkMode;
                updateTheme();
                flashDisplay();
            }
        });

        // Điều hướng lịch sử
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "historyUp");
        actionMap.put("historyUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JList<?> historyList = ((JList<?>) ((JScrollPane) historyPanel.getComponent(1)).getViewport().getView());
                int selectedIndex = historyList.getSelectedIndex();
                if (selectedIndex > 0) {
                    historyList.setSelectedIndex(selectedIndex - 2);
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "historyDown");
        actionMap.put("historyDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JList<?> historyList = ((JList<?>) ((JScrollPane) historyPanel.getComponent(1)).getViewport().getView());
                int selectedIndex = historyList.getSelectedIndex();
                if (selectedIndex < historyListModel.getSize() - 1) {
                    historyList.setSelectedIndex(selectedIndex + 2);
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_DOWN_MASK), "deleteHistory");
        actionMap.put("deleteHistory", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JList<?> historyList = ((JList<?>) ((JScrollPane) historyPanel.getComponent(1)).getViewport().getView());
                int selectedIndex = historyList.getSelectedIndex();
                if (selectedIndex != -1) {
                    int historyIndex = selectedIndex / 2;
                    if (selectedIndex % 2 == 0) {
                        history.remove(historyIndex);
                        historyListModel.remove(selectedIndex + 1);
                        historyListModel.remove(selectedIndex);
                    } else {
                        history.remove(historyIndex);
                        historyListModel.remove(selectedIndex);
                        historyListModel.remove(selectedIndex - 1);
                    }
                    saveHistoryToFile();
                }
            }
        });
    }

    private void showHelpDialog() {
    // GHI CHÚ: Hiển thị dialog chứa danh sách phím tắt, bao gồm chuyển đổi góc
    JDialog helpDialog = new JDialog(this, "Phím Tắt", true);
    helpDialog.setLayout(new BorderLayout(10, 10));
    helpDialog.setSize(400, 400); // Tăng kích thước để chứa thêm nội dung
    helpDialog.setLocationRelativeTo(this);

    JTextArea helpText = new JTextArea(
            "Danh sách phím tắt:\n"
            + "0-9: Nhập số\n"
            + ".: Dấu chấm thập phân\n"
            + "Shift +: Cộng\n"
            + "-: Trừ hoặc dấu âm\n"
            + "Shift + 8: Nhân\n"
            + "/: Chia\n"
            + "Shift + 5 hoặc Ctrl + P: Phần trăm\n"
            + "Enter hoặc =: Tính kết quả\n"
            + "Ctrl+E: Xóa số hiện tại (CE)\n"
            + "Backspace: Xóa ký tự trước con trỏ\n"
            + "Delete hoặc Right Arrow: Xóa ký tự sau con trỏ\n"
            + "C hoặc Esc: Xóa toàn bộ (AC)\n"
            + "Ctrl+R: Căn bậc hai\n"
            + "Ctrl+S: Bình phương\n"
            + "Ctrl+F: Giai thừa\n"
            + "Ctrl+L: Log₁₀\n"
            + "Ctrl+N: Ln\n"
            + "Ctrl+Shift+S: Sin\n"
            + "Ctrl+Shift+C: Cos\n"
            + "Ctrl+Shift+T: Tan\n"
            + "Ctrl+Shift+O: Cot\n"
            + "Ctrl+Shift+D: Chuyển Độ sang Radian\n"
            + "Ctrl+Shift+R: Chuyển Radian sang Độ\n"
            + "Ctrl+H: Xóa lịch sử\n"
            + "Ctrl+D: Chuyển chế độ tối\n"
            + "Mũi tên Lên/Xuống: Điều hướng lịch sử\n"
            + "Ctrl+Delete: Xóa mục lịch sử"
    );
    helpText.setEditable(false);
    helpText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    helpDialog.add(new JScrollPane(helpText), BorderLayout.CENTER);

    JButton closeButton = new JButton("Đóng");
    closeButton.addActionListener(e -> helpDialog.dispose());
    helpDialog.add(closeButton, BorderLayout.SOUTH);

    helpDialog.setVisible(true);
}
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            Calculator calc = new Calculator();
            calc.setVisible(true);
        });
    }
}
