/**
 * STEP 1 - A LIBRARY API
 *
 * Alex books room MS-101 by calling a Java method.
 * Same process, same program, no network. The result is a Java object.
 *
 * Run:  java -cp out Step1_LibraryApi
 */
public class Step1_LibraryApi {
    public static void main(String[] args) {
        RoomService service = new RoomService();

        Booking made = service.reserve("10000001", "MS-101");   // <- the API call
        System.out.println("Got back a Java object: " + made);

        System.out.println();
        System.out.println("Now a bad room. A library API reports failure with an EXCEPTION:");
        try {
            service.reserve("10000001", "NO-SUCH");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println();
        System.out.println("Problem: a web page can't call service.reserve().");
        System.out.println("It lives in another process. So we put HTTP in front of it (Steps 2 and 3).");
    }
}
