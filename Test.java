public class Test {
    public static void main(String[] args) {
        System.out.println(ipToLong("8.8.8.8"));
        System.out.println(ipToLong("192.168.1.1"));
        System.out.println(ipToLong("255.255.255.255"));
    }

    public static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result <<= 8;
            result |= Integer.parseInt(parts[i]) & 0xFF;
        }
        return result;
    }
}
