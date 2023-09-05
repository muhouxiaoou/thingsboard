/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class DashboardEdgeTest extends AbstractEdgeTest {

    private static final int MOBILE_ORDER = 5;
    private static final String IMAGE = "data:image/png;base64,iVBORw0KGgoA";

    @Test
    public void testDashboards() throws Exception {
        // create dashboard and assign to edge
        edgeImitator.expectMessageAmount(1);
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("Edge Test Dashboard");
        dashboard.setMobileHide(true);
        dashboard.setImage(IMAGE);
        dashboard.setMobileOrder(MOBILE_ORDER);
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        DashboardUpdateMsg dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardUpdateMsg.getTitle());
        Assert.assertTrue(dashboardUpdateMsg.getMobileHide());
        Assert.assertEquals(IMAGE, dashboardUpdateMsg.getImage());
        Assert.assertEquals(MOBILE_ORDER, dashboardUpdateMsg.getMobileOrder());
        testAutoGeneratedCodeByProtobuf(dashboardUpdateMsg);

        // update dashboard
        edgeImitator.expectMessageAmount(1);
        savedDashboard.setTitle("Updated Edge Test Dashboard");
        savedDashboard = doPost("/api/dashboard", savedDashboard, Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardUpdateMsg.getTitle());

        // unassign dashboard from edge
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());

        // delete dashboard - message expected, it was sent to all edges
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/dashboard/" + savedDashboard.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(1));

        // create dashboard #2 and assign to edge
        edgeImitator.expectMessageAmount(1);
        dashboard = new Dashboard();
        dashboard.setTitle("Edge Test Dashboard #2");
        savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDashboard.getTitle(), dashboardUpdateMsg.getTitle());

        // assign dashboard #2 to customer
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        edgeImitator.expectMessageAmount(1);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Set<ShortCustomerInfo> assignedCustomers =
                JacksonUtil.fromString(dashboardUpdateMsg.getAssignedCustomers(), new TypeReference<>() {});
        Assert.assertNotNull(assignedCustomers);
        Assert.assertFalse(assignedCustomers.isEmpty());
        Assert.assertTrue(assignedCustomers.contains(new ShortCustomerInfo(savedCustomer.getId(), customer.getTitle(), customer.isPublic())));

        // unassign dashboard #2 from customer
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/customer/" + savedCustomer.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        assignedCustomers =
                JacksonUtil.fromString(dashboardUpdateMsg.getAssignedCustomers(), new TypeReference<>() {});
        Assert.assertNotNull(assignedCustomers);
        Assert.assertTrue(assignedCustomers.isEmpty());

        // delete dashboard #2 - messages expected
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/dashboard/" + savedDashboard.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DashboardUpdateMsg);
        dashboardUpdateMsg = (DashboardUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, dashboardUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), dashboardUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), dashboardUpdateMsg.getIdLSB());
    }

    @Test
    public void testSendDashboardToCloud() throws Exception {
        UUID uuid = Uuids.timeBased();

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DashboardUpdateMsg.Builder dashboardUpdateMsgBuilder = DashboardUpdateMsg.newBuilder();
        dashboardUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        dashboardUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        dashboardUpdateMsgBuilder.setTitle("Edge Test Dashboard");
        dashboardUpdateMsgBuilder.setConfiguration("");
        dashboardUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(dashboardUpdateMsgBuilder);
        uplinkMsgBuilder.addDashboardUpdateMsg(dashboardUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        Dashboard dashboard = doGet("/api/dashboard/" + uuid, Dashboard.class);
        Assert.assertNotNull(dashboard);
        Assert.assertEquals("Edge Test Dashboard", dashboard.getName());
    }

    @Test
    public void testSendDeleteEntityViewOnEdgeToCloud() throws Exception {
        Dashboard savedDashboard = saveDashboardOnCloudAndVerifyDeliveryToEdge();

        UplinkMsg.Builder upLinkMsgBuilder = UplinkMsg.newBuilder();
        DashboardUpdateMsg.Builder dashboardDeleteMsgBuilder = DashboardUpdateMsg.newBuilder();
        dashboardDeleteMsgBuilder.setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE);
        dashboardDeleteMsgBuilder.setIdMSB(savedDashboard.getUuidId().getMostSignificantBits());
        dashboardDeleteMsgBuilder.setIdLSB(savedDashboard.getUuidId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(dashboardDeleteMsgBuilder);

        upLinkMsgBuilder.addDashboardUpdateMsg(dashboardDeleteMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(upLinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(upLinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        DashboardInfo dashboardInfo = doGet("/api/dashboard/info/" + savedDashboard.getUuidId(), DashboardInfo.class);
        Assert.assertNotNull(dashboardInfo);
        List<DashboardInfo> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/dashboards?",
                new TypeReference<PageData<DashboardInfo>>() {
                }, new PageLink(100)).getData();
        Assert.assertFalse(edgeAssets.contains(dashboardInfo));
    }

    private Dashboard saveDashboardOnCloudAndVerifyDeliveryToEdge() throws Exception {
        // create dashboard and assign to edge
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(StringUtils.randomAlphanumeric(15));
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        edgeImitator.expectMessageAmount(1); // dashboard message
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DashboardUpdateMsg> dashboardUpdateMsgOpt = edgeImitator.findMessageByType(DashboardUpdateMsg.class);
        Assert.assertTrue(dashboardUpdateMsgOpt.isPresent());
        DashboardUpdateMsg entityViewUpdateMsg = dashboardUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, entityViewUpdateMsg.getMsgType());
        Assert.assertEquals(savedDashboard.getUuidId().getMostSignificantBits(), entityViewUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDashboard.getUuidId().getLeastSignificantBits(), entityViewUpdateMsg.getIdLSB());
        return savedDashboard;
    }

}
