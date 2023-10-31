public class Main {
    static class Base {
        public String name = "Base";
        static String foo() {
            return "Base.foo";
        }
        String getName() {return this.name;}
    }

    static class Bar extends Base {
        public String name = "Bar";
        static String foo() {
            return "Bar.foo";
        }
        String getName() {return name;}
    }

    public static void main(String...args) {
        Base b = new Bar();
        System.out.println(b.name + b.getName());
        System.out.println(b.foo());
    }
}