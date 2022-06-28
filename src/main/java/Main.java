import com.google.gson.Gson;
import org.ergoplatform.UnsignedErgoLikeTransaction;
import org.ergoplatform.UnsignedInput;
import org.ergoplatform.appkit.*;
import org.ergoplatform.appkit.config.ErgoNodeConfig;
import org.ergoplatform.appkit.config.ErgoToolConfig;
import org.ergoplatform.appkit.impl.ErgoTreeContract;
import org.ergoplatform.appkit.impl.ScalaBridge;
import org.ergoplatform.appkit.impl.UnsignedTransactionImpl;
import org.ergoplatform.restapi.client.ErgoTransactionUnsignedInput;
import org.ergoplatform.restapi.client.TransactionSigningRequest;
import org.ergoplatform.restapi.client.UnsignedErgoTransaction;
import scala.Console;
import special.collection.Coll;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
        public static void main(String[] args) throws FileNotFoundException {
        ErgoToolConfig conf = ErgoToolConfig.load("ergotool.json");
        ErgoNodeConfig nodeConf = conf.getNode();
        ErgoClient ergoClient = RestApiErgoClient.create(nodeConf, "https://api-testnet.ergoplatform.com");
        try {
            String txJson = ergoClient.execute((BlockchainContext ctx) -> {
                InputBox stakeStateInput = ctx.getBoxesById((new String[]{"6ee4d5d0dc518d52386cef251efa732680a8600ecac87ee3ad95c7aa1d8282b3"}))[0];
                InputBox stakePoolInput = ctx.getBoxesById((new String[]{"a9322bc7e71916285855645ef65bd873c48e840ca4ca0e341fdb50721ced1375"}))[0];
                InputBox emissionInput = ctx.getBoxesById((new String[]{"0361723110b35154298317f604d645af873060970067b9867c4b835225063a3a"}))[0];
                InputBox incentiveInput = ctx.getBoxesById((new String[]{"33c2e2f9e6c9cbb6f3e8e9290deaee980d8c0ac202f2272cb85cb27bc6344cf1"}))[0];

                ArrayList<InputBox> inputBoxes = new ArrayList<InputBox>() {
                    {
                        add(stakeStateInput);
                        add(stakePoolInput);
                        add(emissionInput);
                        add(incentiveInput);
                    }
                };

                //stakeStateBox.amountStaked = stakeStateBox.amountStaked + (stakePoolBox.emissionAmount - int(stakePoolBox.emissionAmount/100)) - dust
                Coll<Long> stakeStateR4 = (Coll<Long>) stakeStateInput.getRegisters().get(0).getValue();
                Long[] newStakeStateR4 = new Long[]{
                        stakeStateR4.getOrElse(0, 0L) + 271230300,
                        stakeStateR4.getOrElse(1, 0L) + 1L,
                        stakeStateR4.getOrElse(2, 0L),
                        stakeStateR4.getOrElse(3, 0L) + stakeStateR4.getOrElse(4, 0L),
                        stakeStateR4.getOrElse(4, 0L),
                };

                OutBox stakeStateOutput = ctx.newTxBuilder().outBoxBuilder()
                        .creationHeight(257330)
                        .value(1000000)
                        .contract(new ErgoTreeContract(stakeStateInput.getErgoTree(), NetworkType.TESTNET))
                        .tokens(stakeStateInput.getTokens().get(0), stakeStateInput.getTokens().get(1))
                        .registers(ErgoValue.of(newStakeStateR4, ErgoType.longType())).build();

                ErgoToken newStakePoolAmount = new ErgoToken(stakePoolInput.getTokens().get(1).getId(), stakePoolInput.getTokens().get(1).getValue() - 273970000);

                OutBox stakePoolOutput = ctx.newTxBuilder().outBoxBuilder()
                        .creationHeight(257330)
                        .value(1000000)
                        .contract(new ErgoTreeContract(stakePoolInput.getErgoTree(), NetworkType.TESTNET))
                        .tokens(stakePoolInput.getTokens().get(0), newStakePoolAmount)
                        .registers(stakePoolInput.getRegisters().get(0), stakePoolInput.getRegisters().get(1)).build();

                ErgoToken newEmissionAmount = new ErgoToken(stakePoolInput.getTokens().get(1).getId(), 271230300);

                Long[] newEmissionR4 = new Long[]{
                        stakeStateR4.getOrElse(0, 0L),
                        stakeStateR4.getOrElse(1, 0L),
                        stakeStateR4.getOrElse(2, 0L),
                        271230300L,
                };

                OutBox emissionOutput = ctx.newTxBuilder().outBoxBuilder()
                        .creationHeight(257330)
                        .value(1000000)
                        .contract(new ErgoTreeContract(emissionInput.getErgoTree(), NetworkType.TESTNET))
                        .tokens(emissionInput.getTokens().get(0), newEmissionAmount)
                        .registers(ErgoValue.of(newEmissionR4, ErgoType.longType())).build();

                OutBox emissionFeeOutput = ctx.newTxBuilder().outBoxBuilder()
                        .value(1000000)
                        .creationHeight(257330)
                        .contract(Address.create("3Wvo2r7Z8ZMAL2CQJkShN7JPrVDKVDnJqb2Srcs6mKC157cutvLJ").toErgoContract())
                        .tokens(new ErgoToken(stakePoolInput.getTokens().get(1).getId(), 2739700))
                        .build();

                OutBox incentiveOutput = ctx.newTxBuilder().outBoxBuilder()
                        .creationHeight(257330)
                        .value(95000000)
                        .contract(new ErgoTreeContract(incentiveInput.getErgoTree(), NetworkType.TESTNET))
                        .build();

                OutBox txExecutorOutput = ctx.newTxBuilder().outBoxBuilder()
                        .value(3000000)
                        .creationHeight(257330)
                        .contract(Address.create("3WzCt7XyNmxKRkJRLnD5NjwtbPY7hYpbz1xCCcQfqDF62ATeVUnm").toErgoContract())
                        .build();

                UnsignedTransaction unsignedTx = ctx.newTxBuilder()
                        .boxesToSpend(inputBoxes)
                        .fee(1000000)
                        .sendChangeTo(Address.create("3WzCt7XyNmxKRkJRLnD5NjwtbPY7hYpbz1xCCcQfqDF62ATeVUnm").getErgoAddress())
                        .outputs(
                                stakeStateOutput,
                                stakePoolOutput,
                                emissionOutput,
                                emissionFeeOutput,
                                incentiveOutput,
                                txExecutorOutput
                        )
                        .build();

                TransactionSigningRequest signRequest = new TransactionSigningRequest();
                UnsignedErgoTransaction unsignedErgoTx = new UnsignedErgoTransaction();
                UnsignedErgoLikeTransaction unsignedErgoLikeTx = ((UnsignedTransactionImpl)unsignedTx).getTx();
                for (int i = 0; i < unsignedErgoLikeTx.inputs().length(); i++) {
                    UnsignedInput input = unsignedErgoLikeTx.inputs().apply(i);
                    ErgoTransactionUnsignedInput unsignedInput = new ErgoTransactionUnsignedInput();
                    unsignedInput.setBoxId(unsignedTx.getInputs().get(i).getId().toString());
                    unsignedInput.setExtension(new HashMap<>());
                    unsignedErgoTx = unsignedErgoTx.addInputsItem(unsignedInput);
                }
                unsignedErgoTx.setOutputs(Iso.inverseIso(Iso.JListToIndexedSeq(ScalaBridge.isoErgoTransactionOutput())).to(unsignedErgoLikeTx.outputs()));
                signRequest.setTx(unsignedErgoTx);
                Gson gson = new Gson();

                //{ergo_node}/wallet/transaction/sign will be able to sign the json generated here
                Console.println(gson.toJson(signRequest));

                //This fails
                SignedTransaction signedTx = ctx.newProverBuilder().build().sign(unsignedTx);

                return signedTx.toJson(false);
            });

            Console.println(txJson);
        } catch (Exception e) {
            Console.println(e);
        }
    }
}
