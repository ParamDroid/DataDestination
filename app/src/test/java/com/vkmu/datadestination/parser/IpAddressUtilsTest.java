package com.vkmu.datadestination.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IpAddressUtilsTest {

    @Test
    public void extractDestinationIp_removesIpv4PortFromTuple() {
        assertEquals(
                "8.8.8.8",
                IpAddressUtils.extractDestinationIp("[UDP4] 10.2.2.1:53000 -> 8.8.8.8:53")
        );
    }

    @Test
    public void extractDestinationIp_removesUnbracketedIpv6PortFromTuple() {
        assertEquals(
                "2001:4860:4860::8888",
                IpAddressUtils.extractDestinationIp(
                        "[TCP6] fd00:2:2::1:53000 -> 2001:4860:4860::8888:443"
                )
        );
    }

    @Test
    public void extractDestinationIp_readsBracketedIpv6Tuple() {
        assertEquals(
                "2001:4860:4860::8888",
                IpAddressUtils.extractDestinationIp(
                        "[TCP6] [fd00:2:2::1]:53000 -> [2001:4860:4860::8888]:443"
                )
        );
    }

    @Test
    public void extractDestinationIp_keepsPlainIpv6Literal() {
        assertEquals("2001:db8::1", IpAddressUtils.extractDestinationIp("2001:db8::1"));
    }

    @Test
    public void normalizeIpLiteral_convertsIpv4MappedIpv6ToIpv4() {
        assertEquals("8.8.8.8", IpAddressUtils.normalizeIpLiteral("::ffff:8.8.8.8"));
    }

    @Test
    public void isLocalOrPrivate_handlesIpv6Ranges() {
        assertTrue(IpAddressUtils.isLocalOrPrivate("fd00:2:2::1"));
        assertTrue(IpAddressUtils.isLocalOrPrivate("fe80::1"));
        assertTrue(IpAddressUtils.isLocalOrPrivate("::1"));
        assertFalse(IpAddressUtils.isLocalOrPrivate("2001:4860:4860::8888"));
    }

    @Test
    public void extractDestinationIp_rejectsNonIpValues() {
        assertNull(IpAddressUtils.extractDestinationIp("not-an-ip"));
    }
}
