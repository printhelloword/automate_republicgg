package bot.republic.utility;

public class InputValidator {

    public static boolean isInputNumeric(String input) {
        boolean status = false;
        try {
            Long.parseLong(input);
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }
}
