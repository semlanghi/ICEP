package ee.ut.cs.dsg.d2ia.esper;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;
import ee.ut.cs.dsg.example.linearroad.LinearRoadRunnable;
import ee.ut.cs.dsg.example.linearroad.event.SpeedEvent;


public class Main {

    public static void main(String[] args) throws InterruptedException {


        EPCompiler compiler = EPCompilerProvider.getCompiler();


        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(SpeedEvent.class);

        String epl1 = "@name('my-statement') select * from SpeedEvent\n" +
                "match_recognize (\n" +
                "  partition by key" +
                "  measures max(A.value) as value, count(A.value) as num, first(A.key) as key\n" +
                "  after match skip to current row " +
                "  pattern (A{2,}) \n" +
                "  define \n" +
                "   A as A.value >= 50   " +
                ")";

        String epl2 = "@name('my-statement') select * from SpeedEvent\n" +
                "match_recognize (\n" +
                "  partition by key \n" +
                "  measures max(A.value) as value, B.value as b_value, first(A.key) as key \n" +
                "  after match skip to current row " +
                "  pattern (A{2,} B) \n" +
                "  define \n" +
                "   A as (A.value >= 30 and prev(A.value,1) is null) or (A.value > prev(A.value,1))," +
                "   B as true " +
                ")";

        String epl3 = "@name('my-statement') select * from SpeedEvent\n" +
                "match_recognize (\n" +
                "  partition by key \n" +
                "  measures max(A.value) as value, B.value as b_value, first(A.key) as key \n" +
                "  after match skip past last row " +
                "  pattern (A{2,} B) \n" +
                "  define \n" +
                "   A as  (prev(A.value,1) is null) or (Math.abs(A.value - A.firstOf().value) >= 2)," +
                "   B as true " +
                ")";

        String epl = "@name('my-statement') select * from SpeedEvent\n" +
                "match_recognize (\n" +
                "  partition by key \n" +
                "  measures max(A.value) as value, B.value as b_value, first(A.key) as key \n" +
                "  after match skip past last row " +
                "  pattern (A{2,} B) \n" +
                "  define \n" +
                "   A as  (prev(A.value,1) is null) or (A.aggregate(0, (result, res) => result + res.value) / A.countOf() > 67 )," +
                "   B as true " +
                ")";


        CompilerArguments args1 = new CompilerArguments(configuration);

        EPCompiled epCompiled;
        try {
            epCompiled = compiler.compile(epl, args1);
        } catch (EPCompileException ex) {
            // handle exception here
            throw new RuntimeException(ex);
        }

        EPRuntime runtime = EPRuntimeProvider.getDefaultRuntime(configuration);

        EPEventService eventService = runtime.getEventService();
        eventService.clockExternal();

        EPDeployment deployment;
        try {
            deployment = runtime.getDeploymentService().deploy(epCompiled);
        } catch (EPDeployException ex) {
            // handle exception here
            throw new RuntimeException(ex);
        }

        EPStatement statement = runtime.getDeploymentService().getStatement(deployment.getDeploymentId(), "my-statement");

        statement.addListener((newData, oldData, s, r) -> {
            System.out.println(newData[0].getUnderlying());
        });

        new LinearRoadRunnable("", 100, eventService, "SpeedEvent");

    }

}
//b_value is the closing value
//value is the max in the match