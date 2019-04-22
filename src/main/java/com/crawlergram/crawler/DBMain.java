package com.crawlergram.crawler;

import com.crawlergram.crawler.apicallback.ApiCallbackImplemented;
import com.crawlergram.crawler.apimethods.AuthMethods;
import com.crawlergram.crawler.apimethods.DialogsHistoryMethods;
import com.crawlergram.crawler.dipbit.model.User;
import com.crawlergram.crawler.dipbit.service.DbService;
import com.crawlergram.crawler.logs.LogMethods;
import com.crawlergram.db.DBStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.telegram.api.chat.TLAbsChat;
import org.telegram.api.dialog.TLDialog;
import org.telegram.api.engine.ApiCallback;
import org.telegram.api.engine.AppInfo;
import org.telegram.api.engine.TelegramApi;
import org.telegram.api.engine.storage.AbsApiState;
import org.telegram.api.message.TLAbsMessage;
import org.telegram.api.user.TLAbsUser;
import org.telegram.api.user.TLUser;
import org.telegram.bot.kernel.engine.MemoryApiState;
import org.telegram.tl.TLVector;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

    // other
    private static AbsApiState apiState;
    private static AppInfo appInfo;
    private static ApiCallback apiCallback = new ApiCallbackImplemented();
    private static TelegramApi api;
    private static Map<Integer, TLAbsChat> chatsHashMap = new HashMap<>();
    private static Map<Integer, TLAbsUser> usersHashMap = new HashMap<>();
    private static TLVector<TLDialog> dialogs = new TLVector<>();
    private static Map<Integer, TLAbsMessage> messagesHashMap = new HashMap<>();

    private static int invitePageSize = 5;
    private static int inviteLimitPerAccount = 20;
    private static long inviteIntervalMs = 10;
    private static TLUser tlMe;
    private static boolean inviteSelfContact = false;

    private static DbService service = new DbService();

    public static void main(String[] args) throws IOException {
        log.info("start...");

        CommandLine cmd = CliCmdUtil.validate(args);
        if (cmd != null) {
            log.info("parse param succeed.");
            loadConfig();
//            blackListMap = service.loadBlacklist();
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
                    service.outputUserDialog();
                    log.info("output user({}) dialog succeed.", PHONENUMBER);
                    break;
                case CliCmdUtil.OPT_CONTACT:
                    service.outputChannelContact(Integer.valueOf(sourceChannel));
                    log.info("output channel {} contact succeed.", sourceChannel);
                    break;
                case CliCmdUtil.OPT_DIFF:
                    service.outputChannelContactDiff(Integer.valueOf(sourceChannel), Integer.valueOf(targetChannel));
                    log.info("output diff succeed. source({})- target({})", sourceChannel, targetChannel);
                    break;
                case CliCmdUtil.OPT_INVITE:
                    service.inviteContactToChannel(Integer.valueOf(targetChannel), file);
                    log.info("invite user finish.");
                    break;
                case CliCmdUtil.OPT_CLEAR:
                    service.clearAddUserMessage(Integer.valueOf(targetChannel), 1);
                    log.info("clear add user notification finish");
                    break;

            }
            log.info("finish...");
            System.out.println("finish");
        } else {
            log.error("invalid params.");
        }
        System.exit(1);
    }

    private static void loadConfig() throws IOException {
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
        inviteSelfContact = Boolean.valueOf(config.getProperty("invite.self.contact", "false"));
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
        tlMe = (TLUser) AuthMethods.auth(api, apiState, APIKEY, APIHASH, PHONENUMBER, NAME, SURNAME);

        // get all dialogs of user (telegram returns 100 dialogs at maximum, getting by slices)
        DialogsHistoryMethods.getDialogsChatsUsers(api, dialogs, chatsHashMap, usersHashMap, messagesHashMap);

        service.setApi(api);
        service.setChatsHashMap(chatsHashMap);
        service.setUsersHashMap(usersHashMap);
        service.setDialogs(dialogs);
        service.setMessagesHashMap(messagesHashMap);
        service.setInviteIntervalMs(inviteIntervalMs);
        service.setInviteLimitPerAccount(inviteLimitPerAccount);
        service.setInvitePageSize(invitePageSize);
        service.setInviteSelfContact(inviteSelfContact);
        service.setNotifications(notifications);

        User me = new User();
        me.setId(tlMe.getId());
        me.setPhone(PHONENUMBER);
        service.setMe(me);
    }

}
