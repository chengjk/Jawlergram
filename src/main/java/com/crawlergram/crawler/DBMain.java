/*
 * Title: JkMain.java
 * Project: Jawlergram
 * Creator: Georgii Mikriukov
 * 2019
 */

package com.crawlergram.crawler;

import com.crawlergram.crawler.apicallback.ApiCallbackImplemented;
import com.crawlergram.crawler.apimethods.AuthMethods;
import com.crawlergram.crawler.apimethods.ChannelMethods;
import com.crawlergram.crawler.apimethods.DialogsHistoryMethods;
import com.crawlergram.crawler.apimethods.MessageMethods;
import com.crawlergram.crawler.logs.LogMethods;
import com.crawlergram.crawler.output.ConsoleOutputMethods;
import com.crawlergram.crawler.output.FileMethods;
import com.crawlergram.db.DBStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.telegram.api.chat.TLAbsChat;
import org.telegram.api.chat.channel.TLChannel;
import org.telegram.api.dialog.TLDialog;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.engine.storage.AbsApiState;
import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.input.user.TLAbsInputUser;
import org.telegram.api.input.user.TLInputUser;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.message.TLMessage;
import org.telegram.api.user.TLAbsUser;
import org.telegram.api.user.TLUser;
import org.telegram.bot.kernel.engine.MemoryApiState;
import org.telegram.tl.TLVector;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * created by jacky. 2019/3/19 5:11 PM
 */
@Slf4j
public class DBMain {

    // api variables
    private static int APIKEY = -1; // your api keys
    private static String APIHASH = ""; // your api hash
    private static String PHONENUMBER = ""; // your phone number
    private static String API_STATE_FILE = PHONENUMBER + ".session"; // api state is saved to HDD
    private static String DEVICE_MODEL = ""; // model name
    private static String OS = ""; // os name
    private static String VERSION = ""; // version
    private static String LANG_CODE = ""; // language code
    private static String NAME = ""; // name (for signing up)
    private static String SURNAME = ""; // surname (for signing up)

    // db variables
    private static String TYPE = "mongodb"; // type of storage (at the moment, only "mongodb")
    private static String USERNAME = "admin"; // db user
    private static String PASSWORD = "12354"; // db password
    private static String DATABASE_NAME = "telegram"; // db name
    private static String HOST = "127.0.0.1"; // db host
    private static Integer PORT = 27017; // db port
    private static String GRIDFS_BUCKET_NAME = "fs"; // gridFS bucket

    // other
    private static AbsApiState apiState;
    private static AppInfo appInfo;
    private static ApiCallback apiCallback = new ApiCallbackImplemented();
    private static TelegramApi api;
    private static Map<Integer, TLAbsChat> chatsHashMap = new HashMap<>();
    private static Map<Integer, TLAbsUser> usersHashMap = new HashMap<>();
    private static TLVector<TLDialog> dialogs = new TLVector<>();
    private static Map<Integer, TLAbsMessage> messagesHashMap = new HashMap<>();
    private static DBStorage dbStorage;
    private static int invitePageSize = 10;

    public static void main(String[] args) throws IOException {
        CommandLine cmd = CliCmdUtil.validate(args);
        if (cmd != null) {
            initConfig();
            initApiDoAuth();
            String operate = cmd.getOptionValue("operate");
            String sourceChannel = cmd.getOptionValue("sourceChannel");
            String targetChannel = cmd.getOptionValue("targetChannel");

            String file = cmd.getOptionValue("file");
            switch (operate) {
                case CliCmdUtil.OPT_DIALOG:
                    outputUserDialog();
                    break;
                case CliCmdUtil.OPT_CONTACT:
                    outputChannelContact(Integer.valueOf(sourceChannel));
                    break;
                case CliCmdUtil.OPT_DIFF:
                    outputChannelContactDiff(Integer.valueOf(sourceChannel), Integer.valueOf(targetChannel));
                    break;
                case CliCmdUtil.OPT_INVITE:
                    inviteContactToChannel(Integer.valueOf(targetChannel), file);
                    break;
            }
            System.out.println("finish");
        }
        System.exit(1);
    }

    private static void initConfig() throws IOException {
        Properties config = new Properties();
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream("./application.properties");
            config.load(inStream);
        } finally {
            if (inStream != null) {
                inStream.close();
            }
        }
        APIKEY = Integer.valueOf(config.getProperty("apiKey"));
        APIHASH = config.getProperty("apiHash");
        PHONENUMBER = config.getProperty("phoneNum");
        API_STATE_FILE = PHONENUMBER + ".session";
        DEVICE_MODEL = config.getProperty("deviceModel", "PC");
        OS = config.getProperty("os", "mac");
        VERSION = config.getProperty("version", "1");
        LANG_CODE = config.getProperty("langCode", "en");
    }


    private static void initApiDoAuth() {
//        dbStorage = new MongoDBStorage(USERNAME, DATABASE_NAME, PASSWORD, HOST, PORT, GRIDFS_BUCKET_NAME);
        //register loggers (registration is preferable, otherwise - output will be in console)
        LogMethods.registerLogs("logs", false);

        // api state
        apiState = new MemoryApiState(API_STATE_FILE);
        apiState.setPrimaryDc(5);

        // app info set
        appInfo = new AppInfo(APIKEY, DEVICE_MODEL, OS, VERSION, LANG_CODE);

        // init api
        api = new TelegramApi(apiState, appInfo, apiCallback);

        // set api state
        AuthMethods.setApiState(api, apiState);

        // do auth
        AuthMethods.auth(api, apiState, APIKEY, APIHASH, PHONENUMBER, NAME, SURNAME);

        // get all dialogs of user (telegram returns 100 dialogs at maximum, getting by slices)
        DialogsHistoryMethods.getDialogsChatsUsers(api, dialogs, chatsHashMap, usersHashMap, messagesHashMap);

    }


    private static void outputChannelContactDiff(int sourceChannelId, int targetChannelId) {
        TLVector<TLAbsUser> source = getChannelUsers(sourceChannelId);
        TLVector<TLAbsUser> target = getChannelUsers(targetChannelId);
        Set<Integer> sourceIds = source.stream().map(m -> m.getId()).collect(Collectors.toSet());
        Set<Integer> targetIds = target.stream().map(m -> m.getId()).collect(Collectors.toSet());
        sourceIds.removeAll(targetIds);
        String fileNameFormat = "./%s_rm_%s.contact";
        String fileName = String.format(fileNameFormat, sourceChannelId, targetChannelId);
        source.stream()
                .filter(f -> sourceIds.contains(f.getId()))
                .forEach(f -> {
                    TLUser user = (TLUser) f;
                    String record = user.getId() + "," + user.getAccessHash() + "," +
                            user.getUserName() + "," + user.getFirstName() + "," +
                            user.getLastName() + "," + user.getPhone() + "," + user.getLangCode() + "\n";
                    FileMethods.appendBytesToFile(fileName, record.getBytes());
                });

    }

    private static void outputChannelContact(int channelId) {
        TLVector<TLAbsUser> users = getChannelUsers(channelId);
        for (TLAbsUser u : users) {
            TLUser user = (TLUser) u;
            String record = user.getId() + "," + user.getAccessHash() + "," +
                    user.getUserName() + "," + user.getFirstName() + "," +
                    user.getLastName() + "," + user.getPhone() + "," + user.getLangCode() + "\n";
            String fileNameFormat = "./channel_%s.contact";
            FileMethods.appendBytesToFile(String.format(fileNameFormat, channelId), record.getBytes());
        }
    }

    private static void outputUserDialog() {

        // output user dialogs
        for (TLDialog dialog : dialogs) {
            String fileNameFormat = "./%s.dialog";
            String record = ConsoleOutputMethods.getDialogFullNameWithID(dialog.getPeer().getId(), chatsHashMap, usersHashMap) + "\n";
            FileMethods.appendBytesToFile(String.format(fileNameFormat, PHONENUMBER), record.getBytes());
        }
    }

    public static void inviteContactToChannel(int channelId, String filePath) {
        try {
            File file = new File(filePath);
            TLVector<TLAbsInputUser> users = new TLVector<>();
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.isEmpty()) {
                        String[] split = line.split(",");
                        int userId = Integer.parseInt(split[0]);
                        long userAccessHash = Long.parseLong(split[1]);
                        TLInputUser inputUser = new TLInputUser();
                        inputUser.setUserId(userId);
                        inputUser.setAccessHash(userAccessHash);
                        users.add(inputUser);
                    }
                }
                br.close();
            }

            List<TLAbsInputUser> invited = new ArrayList<>();
            while (invited.size() < users.size()) {
                int pageSize = invitePageSize;
                if (users.size() < invited.size() + invitePageSize) {
                    pageSize = users.size() - invited.size();
                }
                List<TLAbsInputUser> slice = users.subList(invited.size(), invited.size() + pageSize);
                TLVector<TLAbsInputUser> sub = new TLVector<>();
                sub.addAll(slice);
                inviteUserToChannelBatch(channelId, sub);
                clearAddUserMessage(channelId, pageSize);
                invited.addAll(slice);
                System.out.println("invited " + invited.size());
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private static void inviteUserToChannelBatch(int channelId, TLVector<TLAbsInputUser> users) {
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());
        ChannelMethods.inviteUsers(api, inputChannelTo, users);
    }

    private static void inviteUserToChannel(int channelId, int userId, long userAccessHash) {
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());


        TLInputUser user = new TLInputUser();
        user.setUserId(userId);
        user.setAccessHash(userAccessHash);
        ChannelMethods.inviteUser(api, inputChannelTo, user);
    }


    public static void clearAddUserMessage(int channelId, int limit) {
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());
        Set<TLAbsMessage> history = MessageMethods.getHistory(api, inputChannelTo, limit);
        if (history != null) {
            Set<Integer> ids = history.stream().map(m -> ((TLMessage) m).getId()).collect(Collectors.toSet());
            MessageMethods.deleteMessage(api,inputChannelTo,ids);
        }

    }

    private static TLVector<TLAbsUser> getChannelUsers(int channelId) {
        TLChannel channelFrom = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelFrom = new TLInputChannel();
        inputChannelFrom.setChannelId(channelFrom.getId());
        inputChannelFrom.setAccessHash(channelFrom.getAccessHash());
        return ChannelMethods.getUsers(api, inputChannelFrom);
    }
}
