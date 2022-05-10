package org.bvw;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    private static Logger logger = LogManager.getLogger();
    private Properties properties;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private CommandHandler commandHandler = null;

    private EvictingQueue<ChatMessage> chatMessagesHistory = EvictingQueue.create(20);

    private Pattern timestampPattern = Pattern.compile("(\\[\\d{2}:\\d{2}:\\d{2}\\])");
    private Pattern channelAndUserPattern = Pattern.compile("\\[.*?\\] \\[(.*?)\\]:");
    private Pattern commandPattern = null;

    public static void main(String[] args) throws InterruptedException, IOException, AWTException {
        Main m = new Main();
        m.receiveMessages();
    }

    public Main() throws IOException, AWTException {
        properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        commandPattern = Pattern.compile(properties.getProperty("commandPattern", "pango:(\\w{4}\\d{1,2})"));
        logger.info("loaded properties");
        commandHandler = new CommandHandler(properties);
    }

    public void receiveMessages() {
        int port = Integer.valueOf(properties.getProperty("port", "1234"));
        logger.info("starting to listen on " + port);
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://*:"+port);

            while(!Thread.currentThread().isInterrupted()) {
                String message = new String(socket.recv(), ZMQ.CHARSET);
                logger.trace("received: " + message);
                socket.send("Ack".getBytes(ZMQ.CHARSET));
                handleOcrMessage(message);
            }
        }
    }

    private void handleOcrMessage(String ocrMessage) {
        logger.trace("handling message: " + ocrMessage);
        List<ChatMessage> individualMessages = getIndividualChatMessages(ocrMessage);

        individualMessages.removeIf(cm -> chatMessagesHistory.contains(cm));
        individualMessages.stream()
                .filter(this::hasCommand)
                .map(this::getCommand)
                .forEach(this::processCommand);

        individualMessages.forEach(cm -> chatMessagesHistory.add(cm));
    }

    private boolean hasCommand(ChatMessage message) {
        Matcher m = commandPattern.matcher(message.message);
        return m.find();
    }

    private String getCommand(ChatMessage message) {
        Matcher m = commandPattern.matcher(message.message);
        if(m.find()) {
            logger.info("found command: " + m.group(1));
            return m.group(1);
        }
        logger.info("found no command");
        return "";
    }

    private void processCommand(String command) {
        logger.info("processing command: " + command);
        commandHandler.processCommand(command);
    }

    private List<ChatMessage> getIndividualChatMessages(String ocrMessage) {
        logger.trace("splitting message: " + ocrMessage);
        String withoutNewlines = ocrMessage.replace("\n", "");
        String newLineBeforeTimestamp = withoutNewlines.replaceAll(timestampPattern.pattern(), "\n$1");
        newLineBeforeTimestamp = newLineBeforeTimestamp.substring(newLineBeforeTimestamp.indexOf('\n')+1);
        String[] split = newLineBeforeTimestamp.split("\n");
        logger.trace("split message into: " + Arrays.asList(split));
        List<ChatMessage> chatMessageList = Arrays.stream(split)
                .map(s -> makeChatMessage(s))
                .filter( cm -> cm != null)
                .collect(Collectors.toList());
        return chatMessageList;
    }

    private ChatMessage makeChatMessage(String chatMessageWithTimestamp) {
        Matcher mTimestamp = timestampPattern.matcher(chatMessageWithTimestamp);
        if(!mTimestamp.find()) {
            logger.warn("couldnt parse chatmessage from " + chatMessageWithTimestamp);
            return null;
        }
        String timestamp = mTimestamp.group(1);
        String message = chatMessageWithTimestamp.replace(timestamp, "");
        Matcher mUser = channelAndUserPattern.matcher(message);
        String user = "";
        if(!mUser.find()) {
            logger.warn("Couldnt find user in chatmessage, leaving empty");
        } else {
            user = mUser.group(1);
            String fullMatch = mUser.group();
            // replacing with fullMatch since we also want to get rid of the channel name
            message = message.replace(fullMatch, "");
        }
        return new ChatMessage(timestamp, message, user);
    }
}