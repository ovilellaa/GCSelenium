package tests;



import org.testng.annotations.Test;


public class LoginTest extends ClassBaseTest {


    @Test
    public void userLogin() {
        LoginAsDoctor();
        Logout();
    }


}