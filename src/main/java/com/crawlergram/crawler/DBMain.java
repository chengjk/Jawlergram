/*
 * Title: JkMain.java
 * Project: Jawlergram
 * Creator: Georgii Mikriukov
 * 2019
 */

package com.crawlergram.crawler;

import com.alibaba.fastjson.JSON;
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
import org.jetbrains.annotations.NotNull;
import org.telegram.api.channel.TLChannelParticipants;
import org.telegram.api.chat.TLAbsChat;
import org.telegram.api.chat.channel.TLChannel;
import org.telegram.api.dialog.TLDialog;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.RpcException;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.engine.storage.AbsApiState;
import org.telegram.api.input.chat.TLInputChannel;
import org.telegram.api.input.user.TLAbsInputUser;
import org.telegram.api.input.user.TLInputUser;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.message.TLMessageService;
import org.telegram.api.user.TLAbsUser;
import org.telegram.api.user.TLUser;
import org.telegram.api.user.TLUserFull;
import org.telegram.bot.kernel.engine.MemoryApiState;
import org.telegram.tl.TLVector;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private static String API_STATE_FILE = "./sessions/" + PHONENUMBER + ".session"; // api state is saved to HDD
    private static String DEVICE_MODEL = ""; // model name
    private static String OS = ""; // os name
    private static String VERSION = ""; // version
    private static String LANG_CODE = ""; // language code
    private static String NAME = ""; // name (for signing up)
    private static String SURNAME = ""; // surname (for signing up)
    private static boolean notifications = false;


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

    private static int invitePageSize = 5;
    private static int inviteLimitPerAccount = 20;
    private static long inviteIntervalMs = 10;
    public static TLUser me;

    public static void main(String[] args) throws IOException {
        log.info("start...");
        CommandLine cmd = CliCmdUtil.validate(args);
        if (cmd != null) {
            log.info("parse param succeed.");
            initConfig();
            log.info("parse config succeed.");
            log.info("auth succeed.");
            String operate = cmd.getOptionValue("operate");
            String sourceChannel = cmd.getOptionValue("sourceChannel");
            String targetChannel = cmd.getOptionValue("targetChannel");
            if (cmd.hasOption("phone")) {
                //覆盖配置文件的
                PHONENUMBER = cmd.getOptionValue("phone");
            }
            initApiDoAuth();
            String file = cmd.getOptionValue("file");
            switch (operate) {
                case CliCmdUtil.OPT_DIALOG:
                    outputUserDialog();
                    log.info("output user({}) dialog succeed.", PHONENUMBER);
                    break;
                case CliCmdUtil.OPT_CONTACT:
                    outputChannelContact(Integer.valueOf(sourceChannel));
                    log.info("output channel {} contact succeed.", sourceChannel);
                    break;
                case CliCmdUtil.OPT_DIFF:
                    outputChannelContactDiff(Integer.valueOf(sourceChannel), Integer.valueOf(targetChannel));
                    log.info("output diff succeed. source({})- target({})", sourceChannel, targetChannel);
                    break;
                case CliCmdUtil.OPT_INVITE:
                    inviteContactToChannel(Integer.valueOf(targetChannel), file);
                    log.info("invite user succeed.");
                    break;
            }
            log.info("finish...");
            System.out.println("finish");
        } else {
            log.error("invalid params.");
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
        DEVICE_MODEL = config.getProperty("deviceModel", "PC");
        OS = config.getProperty("os", "mac");
        VERSION = config.getProperty("version", "1");
        LANG_CODE = config.getProperty("langCode", "en");
        notifications = Boolean.valueOf(config.getProperty("notifications", "false"));
        inviteIntervalMs = Long.valueOf(config.getProperty("invite.interval.ms", "10"));
        inviteLimitPerAccount = Integer.valueOf(config.getProperty("invite.limit.account", "20"));
    }


    private static void initApiDoAuth() {
//        dbStorage = new MongoDBStorage(USERNAME, DATABASE_NAME, PASSWORD, HOST, PORT, GRIDFS_BUCKET_NAME);
        //register loggers (registration is preferable, otherwise - output will be in console)
        LogMethods.registerLogs("logs", false);
        API_STATE_FILE = "./sessions/" + PHONENUMBER + ".session";

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
        me = (TLUser) AuthMethods.auth(api, apiState, APIKEY, APIHASH, PHONENUMBER, NAME, SURNAME);

        // get all dialogs of user (telegram returns 100 dialogs at maximum, getting by slices)
        DialogsHistoryMethods.getDialogsChatsUsers(api, dialogs, chatsHashMap, usersHashMap, messagesHashMap);

    }


    private static void outputChannelContactDiff(int sourceChannelId, int targetChannelId) {
        TLVector<TLAbsUser> source = getChannelAllUsers(sourceChannelId);
        log.info("get source({}) channel users finish size:{}", sourceChannelId, source.size());
        TLVector<TLAbsUser> target = getChannelAllUsers(targetChannelId);
        log.info("get target({}) channel users finish size:{}", targetChannelId, target.size());
        Set<Integer> sourceIds = source.stream().map(m -> m.getId()).collect(Collectors.toSet());
        Set<Integer> targetIds = target.stream().map(m -> m.getId()).collect(Collectors.toSet());
        sourceIds.removeAll(targetIds);
        String fileNameFormat = "./contacts/%s_channel_%s_rm_%s.contact";
        String fileName = String.format(fileNameFormat, PHONENUMBER, sourceChannelId, targetChannelId);
        source.stream()
                .filter(f -> sourceIds.contains(f.getId()))
                .forEach(f -> {
                    TLUser user = (TLUser) f;
                    String record = getUserString(user);
                    FileMethods.appendBytesToFile(fileName, record.getBytes());
                });

    }


    private static void outputChannelContact(int channelId) {
        int offset = 0;
        int limit = 200;
        int count = 1;
        List<Integer> users = new TLVector<>();
        while (users.size() < count && users.size() < 10000) { //api 只能拉取前1000个人，测试出来的
            TLChannelParticipants participants = getChannelUsers(channelId, offset, limit);
            count = participants.getCount();
            for (TLAbsUser u : participants.getUsers()) {
                TLUser user = (TLUser) u;
                String record = getUserString(user);
                String fileNameFormat = "./contacts/%s_channel_%s.contact";
                FileMethods.appendBytesToFile(String.format(fileNameFormat, PHONENUMBER, channelId), record.getBytes());
            }
            List<Integer> ids = participants.getUsers().stream().map(m -> m.getId()).collect(Collectors.toList());
            users.addAll(ids);
            offset = users.size();
            log.info("output contact process :channel({}) {}/{} ", channelId, users.size(), count);
        }
    }

    private static void outputUserDialog() {

        // output user dialogs
        for (TLDialog dialog : dialogs) {
            String fileNameFormat = "./dialogs/%s.dialog";
            String record = ConsoleOutputMethods.getDialogFullNameWithID(dialog.getPeer().getId(), chatsHashMap, usersHashMap) + "\n";
            FileMethods.appendBytesToFile(String.format(fileNameFormat, PHONENUMBER), record.getBytes());
        }
    }

    public static void inviteContactToChannel(int channelId, String filePath) {
        try {
            File file = new File(filePath);
            TLVector<TLAbsInputUser> users = new TLVector<>();
            Map<Integer, String> userNameMap = new HashMap<>();
            if (file.exists()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
                String line;
                int readLines = 0;
                while (readLines < inviteLimitPerAccount && (line = br.readLine()) != null) {
                    if (!line.isEmpty()) {
                        readLines++;
                        String[] split = line.split(",");
                        int userId = Integer.parseInt(split[0]);
                        long userAccessHash = Long.parseLong(split[1]);
                        String userName = split[2];
                        userNameMap.put(userId, userName);
                        TLInputUser inputUser = new TLInputUser();
                        inputUser.setUserId(userId);
                        inputUser.setAccessHash(userAccessHash);
                        users.add(inputUser);
                    }
                }
                br.close();
            }

            //one by one
            List<TLAbsInputUser> invited = new ArrayList<>();
            for (TLAbsInputUser user : users) {
                boolean succeed = false;
                String errorCode = "";
                String errorTag = "";
                TimeUnit.MILLISECONDS.sleep(inviteIntervalMs);
                try {
                    inviteUserToChannel(channelId, user);
                    succeed = true;
                } catch (TimeoutException e) {
                    log.error("invite timeout ", e);
                } catch (RpcException e) {
                    errorTag = e.getErrorTag();
                } catch (IOException e) {
                    log.error("invite error ", e);
                } catch (Exception e) {
                    log.error("unknown error ", e);
                }
                invited.add(user);
                log.info("{}! {} {}. {} added id({}) username({}) ",
                        succeed ? "successful" : "failed",
                        errorCode, errorTag, me.getId(), ((TLInputUser) user).getUserId(), userNameMap.get(((TLInputUser) user).getUserId()));
                log.info("invite process: {}/{} ", invited.size(), users.size());
                if (succeed && !notifications) {
                    clearAddUserMessage(channelId, 5);
                }
                if (errorTag.toUpperCase().contains("FLOOD")) {
                    log.error("account({}) FLOOD. {}", me.getId(), errorTag);
                    break;
                }
            }
            //pageable
//            inviteUserToChannelPageable(channelId, users, invitePageSize);
        } catch (Exception e) {
            log.error("unknown error", e);
        }
    }

    @Deprecated
    private static void inviteUserToChannelPageable(int channelId, TLVector<TLAbsInputUser> users, int pageSize) throws IOException, TimeoutException {
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());
        List<TLAbsInputUser> invited = new ArrayList<>();
        while (invited.size() < users.size()) {
            if (users.size() < invited.size() + pageSize) {
                pageSize = users.size() - invited.size();
            }
            List<TLAbsInputUser> slice = users.subList(invited.size(), invited.size() + pageSize);
            TLVector<TLAbsInputUser> sub = new TLVector<>();
            sub.addAll(slice);
            ChannelMethods.inviteUsers(api, inputChannelTo, sub);
            invited.addAll(slice);

            if (!notifications) {
                clearAddUserMessage(channelId, 5);
            }
            log.info("{} added  {}", me.getId(), JSON.toJSONString(sub));
            log.info("invite process: {}/{} ", invited.size(), users.size());
        }
    }

    private static void inviteUserToChannel(int channelId, TLAbsInputUser user) throws IOException, TimeoutException {
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());

        ChannelMethods.inviteUser(api, inputChannelTo, user);
    }


    public static void clearAddUserMessage(int channelId, int limit) {
        TLChannel channelTo = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelTo = new TLInputChannel();
        inputChannelTo.setChannelId(channelTo.getId());
        inputChannelTo.setAccessHash(channelTo.getAccessHash());
        Set<TLAbsMessage> history = MessageMethods.getHistory(api, inputChannelTo, limit);
        if (history != null && !history.isEmpty()) {
            Set<Integer> ids = history.stream().map(m -> ((TLMessageService) m).getId()).collect(Collectors.toSet());
            MessageMethods.deleteMessage(api, inputChannelTo, ids);
        }

    }

    private static TLChannelParticipants getChannelUsers(int channelId, int offset, int limit) {
        TLChannel channelFrom = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelFrom = new TLInputChannel();
        inputChannelFrom.setChannelId(channelFrom.getId());
        inputChannelFrom.setAccessHash(channelFrom.getAccessHash());
        return ChannelMethods.getUsers(api, inputChannelFrom, offset, limit);
    }

    private static TLVector<TLAbsUser> getChannelAllUsers(int channelId) {
        TLChannel channelFrom = (TLChannel) chatsHashMap.get(channelId);
        TLInputChannel inputChannelFrom = new TLInputChannel();
        inputChannelFrom.setChannelId(channelFrom.getId());
        inputChannelFrom.setAccessHash(channelFrom.getAccessHash());
        return ChannelMethods.getAllUsers(api, inputChannelFrom);
    }

    @NotNull
    private static String getUserString(TLUser user) {
        return user.getId() + "," + user.getAccessHash() + "," +
                user.getUserName() + "," + user.getFirstName() + "," +
                user.getLastName() + "," + user.getPhone() + "," + user.getLangCode() + "\n";
    }
}
