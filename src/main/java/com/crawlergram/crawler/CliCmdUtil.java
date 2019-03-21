
package com.crawlergram.crawler;

import org.apache.commons.cli.*;

/**
 * created by jacky. 2019/3/21 2:42 PM
 */
public class CliCmdUtil {

    public static final String o = "operate";
    public static final String sc = "sourceChannel";
    public static final String tc = "targetChannel";
    public static final String f = "file";


    public static final String OPT_DIALOG = "dialog";
    public static final String OPT_CONTACT = "contact";
    public static final String OPT_DIFF = "diff";
    public static final String OPT_INVITE = "invite";


    public static CommandLine validate(String[] args) {

        Options options = new Options();
        Option operate = new Option("o", o, true, "type of operate:\n" +
                OPT_DIALOG + ": output user dialog,\n" +
                OPT_CONTACT + ": output channel contact,\n" +
                OPT_INVITE + ": invite contact to target channel,\n" +
                OPT_DIFF + ": output diff contact between source channel and target chanel. source-target.");
        operate.setRequired(true);
        Option sourceChannel = new Option("sc", sc, true, "channel id");
        Option targetChannel = new Option("tc", tc, true, "channel id");
        Option file = new Option("f", f, true, "contact file,output by diff. required for invite.");


        options.addOption(operate);
        options.addOption(sourceChannel);
        options.addOption(targetChannel);
        options.addOption(file);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);

            String type = cmd.getOptionValue("operate");

            switch (type) {
                case OPT_DIALOG:
                    break;
                case OPT_CONTACT:
                    checkParam(cmd, sc);
                    break;
                case OPT_DIFF:
                    checkParam(cmd, sc);
                    checkParam(cmd, tc);
                    break;
                case OPT_INVITE:
                    checkParam(cmd, tc);
                    checkParam(cmd, f);
                    break;
                default:
                    throw new ParseException("unknown operate.");
            }


        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("telTool", options);
            return null;
        }
        return cmd;

    }

    private static void checkParam(CommandLine cmd, String name) throws ParseException {
        if (!cmd.hasOption(name)) {
            throw new ParseException(name + " is required!");
        }
    }


}