package com.zimbra.ssdb;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import static org.easymock.EasyMock.*;

import org.easymock.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ephemeral.EphemeralInput;
import com.zimbra.cs.ephemeral.EphemeralKey;
import com.zimbra.cs.ephemeral.EphemeralLocation;
import com.zimbra.cs.ephemeral.EphemeralResult;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.InMemoryEphemeralStore;
import com.zimbra.cs.ephemeral.EphemeralInput.Expiration;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class SSDBEphemeralStoreTest {

    @Mock
    private JedisPool mockJedisPool;
    
    @Mock
    private Jedis jedis;
    
    @Before
    public void setUp() throws Exception {
        jedis = EasyMock.mock(Jedis.class);
        mockJedisPool = EasyMock.mock(JedisPool.class);
        MailboxTestUtil.initServer("../zm-store/");
        Provisioning.getInstance().getConfig().setEphemeralBackendURL("ssdb:localhost:8888");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testFactory() throws ServiceException {
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        EphemeralStore store = SSDBEphemeralStore.getFactory().getStore();
        assertTrue(store instanceof SSDBEphemeralStore);
    }
    
    @Test
    public void testShutdown() throws ServiceException {
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        EphemeralStore store = SSDBEphemeralStore.getFactory().getStore();
        assertTrue(store instanceof SSDBEphemeralStore);

        ((SSDBEphemeralStore)store).setPool(mockJedisPool);
        mockJedisPool.close();
        mockJedisPool.destroy();
        expectLastCall().once();
        replay(mockJedisPool);
        SSDBEphemeralStore.getFactory().shutdown();
        verify(mockJedisPool);
    }

    @Test
    public void testGet() throws ServiceException {
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        EphemeralStore store = SSDBEphemeralStore.getFactory().getStore();
        assertTrue(store instanceof SSDBEphemeralStore);
        
        ((SSDBEphemeralStore)store).setPool(mockJedisPool);
        EphemeralLocation cosLocation = new EphemeralLocation() {
            @Override
            public String[] getLocation() { return new String[] { "cos", "47e456be-b00a-465e-a1db-4b53e64fa" }; }
        };
        expect(mockJedisPool.getResource()).andReturn(jedis).atLeastOnce();
        expect(jedis.get("cos|47e456be-b00a-465e-a1db-4b53e64fa|somekey")).andReturn(null);
        jedis.close();
        replay(mockJedisPool);
        replay(jedis);
        EphemeralKey eKey = new EphemeralKey("somekey");
        store.get(eKey, cosLocation);
        verify(mockJedisPool);
        verify(jedis);
    }
    
    @Test
    public void testSetDynamic() throws ServiceException {
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        EphemeralStore store = SSDBEphemeralStore.getFactory().getStore();
        assertTrue(store instanceof SSDBEphemeralStore);
        
        EphemeralKey eKey = new EphemeralKey("testK", "testD");
        EphemeralInput kv = new EphemeralInput(eKey,"testV");
        ((SSDBEphemeralStore)store).setPool(mockJedisPool);
        EphemeralLocation domainLocation = new EphemeralLocation() {
            @Override
            public String[] getLocation() { return new String[] { "domain", "47e456be-b00a-465e-a1db-4b53e64fa" }; }
        };
        expect(mockJedisPool.getResource()).andReturn(jedis).atLeastOnce();
        expect(jedis.set("domain|47e456be-b00a-465e-a1db-4b53e64fa|testK|testD","testV")).andReturn("testK");
        jedis.close();
        replay(mockJedisPool);
        replay(jedis);
        store.set(kv, domainLocation);
        verify(mockJedisPool);
        verify(jedis);
    }
    
    @Test
    public void testSetNonDynamic() throws ServiceException {
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        EphemeralStore store = SSDBEphemeralStore.getFactory().getStore();
        assertTrue(store instanceof SSDBEphemeralStore);
        
        EphemeralKey eKey = new EphemeralKey("testK");
        EphemeralInput kv = new EphemeralInput(eKey,"testV");
        ((SSDBEphemeralStore)store).setPool(mockJedisPool);
        EphemeralLocation domainLocation = new EphemeralLocation() {
            @Override
            public String[] getLocation() { return new String[] { "domain", "47e456be-b00a-465e-a1db-4b53e64fa" }; }
        };
        expect(mockJedisPool.getResource()).andReturn(jedis).atLeastOnce();
        expect(jedis.set("domain|47e456be-b00a-465e-a1db-4b53e64fa|testK","testV")).andReturn("testK");
        jedis.close();
        replay(mockJedisPool);
        replay(jedis);
        store.set(kv, domainLocation);
        verify(mockJedisPool);
        verify(jedis);
    }
    
    @Test
    public void testLastLogonTimestampToKey() throws ServiceException {
        String lastLogonTime = "20160912212057.178Z";   
        EphemeralKey eKey = new EphemeralKey(Provisioning.A_zimbraLastLogonTimestamp);
        EphemeralInput input = new EphemeralInput(eKey, lastLogonTime);
        EphemeralLocation accountIDLocation = new EphemeralLocation() {
            @Override
            public String[] getLocation() { return new String[] { "account", "47e456be-b00a-465e-a1db-4b53e64fa" }; }
        };
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        SSDBEphemeralStore store = (SSDBEphemeralStore)SSDBEphemeralStore.getFactory().getStore();
        
        assertEquals("account|47e456be-b00a-465e-a1db-4b53e64fa|zimbraLastLogonTimestamp", store.toKey(input, accountIDLocation));
    }
    
    @Test
    public void testLastLogonTimestampToValue() throws ServiceException {
        String lastLogonTime = "20160912212057.178Z";   
        EphemeralKey eKey = new EphemeralKey(Provisioning.A_zimbraLastLogonTimestamp);
        EphemeralInput input = new EphemeralInput(eKey, lastLogonTime);
        EphemeralLocation accountIDLocation = new EphemeralLocation() {
            @Override
            public String[] getLocation() { return new String[] { "account", "47e456be-b00a-465e-a1db-4b53e64fa" }; }
        };
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        SSDBEphemeralStore store = (SSDBEphemeralStore)SSDBEphemeralStore.getFactory().getStore();
        assertEquals(lastLogonTime, store.toValue(input, accountIDLocation));
    }
    
    @Test
    public void testAuthTokenToKey() throws ServiceException {
        Expiration exp = new Expiration(1473761137744L, TimeUnit.MILLISECONDS);
        EphemeralKey eKey = new EphemeralKey(Provisioning.A_zimbraAuthTokens, "366778080");
        EphemeralInput input = new EphemeralInput(eKey, "8.7.0_GA_1659", exp);
        EphemeralLocation accountIDLocation = new EphemeralLocation() {
            @Override
            public String[] getLocation() { return new String[] { "account", "47e456be-b00a-465e-a1db-4b53e64fa" }; }
        };
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        SSDBEphemeralStore store = (SSDBEphemeralStore)SSDBEphemeralStore.getFactory().getStore();
        assertEquals("account|47e456be-b00a-465e-a1db-4b53e64fa|zimbraAuthTokens|366778080", store.toKey(input, accountIDLocation));
    }
    
    @Test
    public void testAuthTokenToValue() throws ServiceException {
        Expiration exp = new Expiration(1473761137744L, TimeUnit.MILLISECONDS);
        EphemeralKey eKey = new EphemeralKey(Provisioning.A_zimbraAuthTokens, "8.7.0_GA_1659");
        EphemeralInput input = new EphemeralInput(eKey, "8.7.0_GA_1659", exp);
        EphemeralLocation accountIDLocation = new EphemeralLocation() {
            @Override
            public String[] getLocation() { return new String[] { "account", "47e456be-b00a-465e-a1db-4b53e64fa" }; }
        };
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        SSDBEphemeralStore store = (SSDBEphemeralStore)SSDBEphemeralStore.getFactory().getStore();
        assertEquals("8.7.0_GA_1659", store.toValue(input, accountIDLocation));
    }
    
    @Test
    public void testCsrfTokenToValue() throws ServiceException {
        Expiration exp = new Expiration(1473761137744L, TimeUnit.MILLISECONDS);
        EphemeralKey eKey = new EphemeralKey(Provisioning.A_zimbraCsrfTokenData, "3822663c52f27487f172055ddc0918aa");
        EphemeralInput input = new EphemeralInput(eKey, "69643d33363a30666532376439312d656339342d346534352d383436342d3339326262383736313364383b6578703d31333a313437333735383435373138323b7369643d31303a313135303130393434363b", exp);
        EphemeralLocation accountIDLocation = new EphemeralLocation() {
            @Override
            public String[] getLocation() { return new String[] { "account", "47e456be-b00a-465e-a1db-4b53e64fa" }; }
        };
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        SSDBEphemeralStore store = (SSDBEphemeralStore)SSDBEphemeralStore.getFactory().getStore();
        assertEquals("69643d33363a30666532376439312d656339342d346534352d383436342d3339326262383736313364383b6578703d31333a313437333735383435373138323b7369643d31303a313135303130393434363b", store.toValue(input, accountIDLocation));
    }
    
    @Test
    public void testCsrfTokenToKey() throws ServiceException {
        Expiration exp = new Expiration(1473761137744L, TimeUnit.MILLISECONDS);
        EphemeralKey eKey = new EphemeralKey(Provisioning.A_zimbraCsrfTokenData, "3822663c52f27487f172055ddc0918aa");
        EphemeralInput input = new EphemeralInput(eKey, "69643d33363a30666532376439312d656339342d346534352d383436342d3339326262383736313364383b6578703d31333a313437333735383435373138323b7369643d31303a313135303130393434363b", exp);

        EphemeralLocation accountIDLocation = new EphemeralLocation() {
            @Override
            public String[] getLocation() { return new String[] { "account", "47e456be-b00a-465e-a1db-4b53e64fa" }; }
        };
        EphemeralStore.setFactory(SSDBEphemeralStore.Factory.class);
        SSDBEphemeralStore store = (SSDBEphemeralStore)SSDBEphemeralStore.getFactory().getStore();
        assertEquals("account|47e456be-b00a-465e-a1db-4b53e64fa|zimbraCsrfTokenData|3822663c52f27487f172055ddc0918aa", store.toKey(input, accountIDLocation));
    }
}
