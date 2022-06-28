package org.jh.tools.daml;

import com.daml.ledger.api.v1.ActiveContractsServiceGrpc;
import com.daml.ledger.api.v1.ActiveContractsServiceOuterClass;
import com.daml.ledger.api.v1.LedgerIdentityServiceGrpc;
import com.daml.ledger.api.v1.LedgerIdentityServiceOuterClass;
import com.daml.ledger.api.v1.TransactionFilterOuterClass;
import com.daml.ledger.api.v1.admin.UserManagementServiceGrpc;
import com.daml.ledger.api.v1.admin.UserManagementServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Iterator;

public class DamlLedgerQuery
{
    private final ManagedChannel channel;

    private DamlLedgerQuery(ManagedChannel channel)
    {
        this.channel = channel;
    }

    public static DamlLedgerQuery createDamlLedgerQuery(String host, int port)
    {
        // Initialize a plaintext gRPC channel
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        return new DamlLedgerQuery(channel);
    }

    public int queryActiveContracts(String partyId)
    {
        // fetch the ledger ID, which is used in subsequent requests sent to the ledger
        String ledgerId = fetchLedgerId(channel);

        // fetch the party IDs that got created in the Daml init script
        //String bobParty = fetchPartyId(channel, BOB_USER);

        ActiveContractsServiceGrpc.ActiveContractsServiceBlockingStub activeContractsService =
                ActiveContractsServiceGrpc.newBlockingStub(channel);

        ActiveContractsServiceOuterClass.GetActiveContractsRequest request = ActiveContractsServiceOuterClass.
                GetActiveContractsRequest.newBuilder()
                            .setLedgerId(ledgerId)
                .setFilter(TransactionFilterOuterClass.TransactionFilter.newBuilder().putFiltersByParty(partyId, TransactionFilterOuterClass.Filters.newBuilder().build()).build()).build();
        Iterator<ActiveContractsServiceOuterClass.GetActiveContractsResponse> result = activeContractsService.getActiveContracts(request);
        ActiveContractsServiceOuterClass.GetActiveContractsResponse response = result.next();
        return response.getActiveContractsCount();
    }



    /**
     * Fetches the ledger id via the Ledger Identity Service.
     *
     * @param channel the gRPC channel to use for services
     * @return the ledger id as provided by the ledger
     */
    private static String fetchLedgerId(ManagedChannel channel) {
        LedgerIdentityServiceGrpc.LedgerIdentityServiceBlockingStub ledgerIdService = LedgerIdentityServiceGrpc.newBlockingStub(channel);
        LedgerIdentityServiceOuterClass.GetLedgerIdentityResponse identityResponse = ledgerIdService.getLedgerIdentity(LedgerIdentityServiceOuterClass.GetLedgerIdentityRequest.getDefaultInstance());
        return identityResponse.getLedgerId();
    }

    private static String fetchPartyId(ManagedChannel channel, String userId) {
        UserManagementServiceGrpc.UserManagementServiceBlockingStub userManagementService = UserManagementServiceGrpc.newBlockingStub(channel);
        UserManagementServiceOuterClass.GetUserResponse getUserResponse = userManagementService.getUser(UserManagementServiceOuterClass.GetUserRequest.newBuilder().setUserId(userId).build());
        return getUserResponse.getUser().getPrimaryParty();
    }
}
