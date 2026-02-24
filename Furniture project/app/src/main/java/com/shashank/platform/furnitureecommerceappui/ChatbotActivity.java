package com.shashank.platform.furnitureecommerceappui;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.shashank.platform.furnitureecommerceappui.adapters.ChatAdapter;
import com.shashank.platform.furnitureecommerceappui.models.Message;
import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private EditText messageInput;
    private View sendButton;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        chatRecyclerView = findViewById(R.id.chat_recycler_view);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        backButton = findViewById(R.id.chatbot_back_button);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        backButton.setOnClickListener(v -> finish());

        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();
            if (!messageText.isEmpty()) {
                sendMessage(messageText);
            }
        });

        addBotMessage("Hello! I'm your Furniture Assistant. 🛋️ How can I help you today?");
        addBotMessage("You can ask me about:\n• Order status\n• Returns & Refunds\n• Payment options\n• Product quality\n• Latest offers");
    }

    private void sendMessage(String messageText) {
        messageList.add(new Message(messageText, true));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        messageInput.setText("");
        chatRecyclerView.scrollToPosition(messageList.size() - 1);

        // Simulate "typing..." delay
        new Handler().postDelayed(() -> {
            String botResponse = getBotResponse(messageText);
            addBotMessage(botResponse);
        }, 1000);
    }

    private void addBotMessage(String messageText) {
        messageList.add(new Message(messageText, false));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }

    private String getBotResponse(String userInput) {
        String input = userInput.toLowerCase();

        if (input.contains("order status") || input.contains("track") || input.contains("where is my order")) {
            return "You can track your order in the 'My Orders' section of your profile. Usually, furniture takes 5-7 business days to deliver.";
        } else if (input.contains("return") || input.contains("refund") || input.contains("cancel")) {
            return "We have a 30-day easy return policy. If you're not satisfied, you can initiate a return from the Order Details page.";
        } else if (input.contains("payment") || input.contains("pay") || input.contains("cod")) {
            return "We accept Credit/Debit Cards, UPI, and Cash on Delivery (COD) for most locations.";
        } else if (input.contains("discount") || input.contains("offer") || input.contains("coupon") || input.contains("sale")) {
            return "Great news! Currently, we have a 'New Home' sale with up to 40% off on all sofa sets. Use code: HOME40.";
        } else if (input.contains("material") || input.contains("quality") || input.contains("wood")) {
            return "We use premium quality Teak and Sheesham wood for our furniture. All products come with a 1-year warranty.";
        } else if (input.contains("hello") || input.contains("hi") || input.contains("hey")) {
            return "Hi there! Looking for some beautiful furniture today?";
        } else if (input.contains("contact") || input.contains("customer care") || input.contains("support")) {
            return "You can reach our support team at support@furnitureapp.com or call us at 1800-FURNITURE (Mon-Sat, 9 AM - 6 PM).";
        } else if (input.contains("address") || input.contains("location")) {
            return "You can manage your delivery addresses in the 'Saved Addresses' section of your profile.";
        } else if (input.contains("thanks") || input.contains("thank you")) {
            return "You're welcome! Happy shopping! 🏠";
        } else {
            return "I'm not sure I understand. Could you please rephrase? You can ask about orders, returns, payments, or current offers.";
        }
    }
}
