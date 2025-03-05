package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.net.*;
import java.util.*;

/**
 * @author linjpxc
 */
@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
public final class IpUtils {
    private IpUtils() {
    }

    private static final Lazy<InetAddress> PUBLIC_ADDRESS = Lazy.of(IpUtils::getPublicAddress0);
    private static final Lazy<List<? extends InetAddress>> ALL_LOCAL_ADDRESS = Lazy.of(IpUtils::getAllLocalAddress0);
    private static final Lazy<List<? extends Inet4Address>> ALL_LOCAL_ADDRESS4 = Lazy.of(IpUtils::getAllLocalAddress4_0);
    private static final Lazy<List<? extends Inet6Address>> ALL_LOCAL_ADDRESS6 = Lazy.of(IpUtils::getAllLocalAddress6_0);
    private static final Lazy<InetAddress> LOCAL_ADDRESS = Lazy.of(IpUtils::getLocalAddress0);
    private static final Lazy<Inet4Address> LOCAL_ADDRESS4 = Lazy.of(IpUtils::getLocalAddress4_0);
    private static final Lazy<Inet6Address> LOCAL_ADDRESS6 = Lazy.of(IpUtils::getLocalAddress6_0);

    public static InetAddress getPublicAddress() {
        return PUBLIC_ADDRESS.get();
    }

    public static InetAddress getLocalAddress() {
        return getLocalAddress(false);
    }

    public static InetAddress getLocalAddress(boolean realtime) {
        if (realtime) {
            return getLocalAddress0();
        }
        return LOCAL_ADDRESS.get();
    }

    public static Inet4Address getLocalAddress4() {
        return getLocalAddress4(false);
    }

    public static Inet4Address getLocalAddress4(boolean realtime) {
        if (realtime) {
            return getLocalAddress4_0();
        }
        return LOCAL_ADDRESS4.get();
    }

    public static Inet6Address getLocalAddress6() {
        return getLocalAddress6(false);
    }

    public static Inet6Address getLocalAddress6(boolean realtime) {
        if (realtime) {
            return getLocalAddress6_0();
        }
        return LOCAL_ADDRESS6.get();
    }

    @Nonnull
    public static List<? extends InetAddress> getAllLocalAddress() {
        return getAllLocalAddress(false);
    }

    @Nonnull
    public static List<? extends InetAddress> getAllLocalAddress(boolean realtime) {
        if (realtime) {
            return getAllLocalAddress0();
        }
        return ALL_LOCAL_ADDRESS.get();
    }

    @Nonnull
    public static List<? extends Inet4Address> getAllLocalAddress4() {
        return getAllLocalAddress4(false);
    }

    @Nonnull
    public static List<? extends Inet4Address> getAllLocalAddress4(boolean realtime) {
        if (realtime) {
            return getAllLocalAddress4_0();
        }
        return ALL_LOCAL_ADDRESS4.get();
    }

    @Nonnull
    public static List<? extends Inet6Address> getAllLocalAddress6() {
        return getAllLocalAddress6(false);
    }

    @Nonnull
    public static List<? extends Inet6Address> getAllLocalAddress6(boolean realtime) {
        if (realtime) {
            return getAllLocalAddress6_0();
        }
        return ALL_LOCAL_ADDRESS6.get();
    }

    private static InetAddress getLocalAddress0() {
        final Inet4Address address = getLocalAddress4_0();
        if (address != null) {
            return address;
        }
        return getLocalAddress6_0();
    }

    private static Inet4Address getLocalAddress4_0() {
        final Map<InetAddress, InetAddress> map = getAllLocalAddressAndBroadcast();
        Inet4Address result = null;
        for (InetAddress address : map.keySet()) {
            if (address instanceof Inet4Address) {
                final InetAddress broadcast = map.get(address);
                if ("0.0.0.0".equals(broadcast.getHostAddress())) {
                    return (Inet4Address) address;
                }
                if (result == null) {
                    result = (Inet4Address) address;
                }
            }
        }
        return result;
    }

    private static Inet6Address getLocalAddress6_0() {
        final Map<InetAddress, InetAddress> map = getAllLocalAddressAndBroadcast();
        for (InetAddress inetAddress : map.keySet()) {
            if (inetAddress instanceof Inet6Address) {
                return (Inet6Address) inetAddress;
            }
        }
        return null;
    }

    private static Map<InetAddress, InetAddress> getAllLocalAddressAndBroadcast() {
        final Map<InetAddress, InetAddress> map = new HashMap<>();
        try {

            final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            // 遍历所有网络接口
            while (networkInterfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = networkInterfaces.nextElement();
                // 如果此网络接口为 回环接口 或者 虚拟接口(子接口) 或者 未启用 或者 描述中包含VM
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    // 继续下次循环
                    continue;
                }
                List<InterfaceAddress> list = networkInterface.getInterfaceAddresses();
                if (list != null) {
                    for (InterfaceAddress ia : list) {
                        if (ia.getAddress() != null && ia.getBroadcast() != null) {
                            map.put(ia.getAddress(), ia.getBroadcast());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    @Nonnull
    private static List<? extends InetAddress> getAllLocalAddress0() {
        return new ArrayList<>(getAllLocalAddressAndBroadcast().keySet());
    }

    @Nonnull
    private static List<? extends Inet4Address> getAllLocalAddress4_0() {
        final List<Inet4Address> list = new ArrayList<>();
        for (InetAddress address : getAllLocalAddressAndBroadcast().keySet()) {
            if (address instanceof Inet4Address) {
                list.add((Inet4Address) address);
            }
        }
        return list;
    }

    @Nonnull
    private static List<? extends Inet6Address> getAllLocalAddress6_0() {
        final List<Inet6Address> list = new ArrayList<>();
        for (InetAddress address : getAllLocalAddressAndBroadcast().keySet()) {
            if (address instanceof Inet6Address) {
                list.add((Inet6Address) address);
            }
        }
        return list;
    }

    private static InetAddress getPublicAddress0() {
        try {
            final URL url = new URL("https://ipinfo.io/ip");
            final URLConnection connection = url.openConnection();
            connection.connect();
            try (InputStream in = connection.getInputStream()) {
                return InetAddress.getByName(new String(StreamUtils.readAllBytes(in)));
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
