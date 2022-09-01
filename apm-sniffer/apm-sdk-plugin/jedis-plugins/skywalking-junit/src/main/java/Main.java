public class Main {

    // target class
    // target method
    //
    public static void main(String[] args) throws Exception {
        Class<?> aClass = Class.forName(args[0]);
        Object testObject = aClass.getConstructor().newInstance();
        aClass.getDeclaredMethod(args[1]).invoke(testObject);
    }
}
