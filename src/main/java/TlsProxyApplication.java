import proxy.ClientTlsProxy;
import proxy.ServerTlsProxy;
import proxy.TlsProxy;
import resources.StringValues;
import utils.Log;

import java.util.ArrayList;
import java.util.List;

public class TlsProxyApplication {
    public static void main(String[] args){
        if(args.length<2){
            Log.error(StringValues.INIT_USAGE_PROMPT);
            return;
        }

        TlsProxy tlsProxy;
        if(args[0].equals("client")){
            tlsProxy=new ClientTlsProxy();
        }
        else if(args[0].equals("server")){
            tlsProxy=new ServerTlsProxy();
        }
        else{
            Log.error(StringValues.INIT_USAGE_PROMPT);
            return;
        }

        int port=0;
        List<Integer> targetPorts=new ArrayList<>();

        for(int i=1;i<args.length;i++){
            int currentPortNumber;
            try{
                currentPortNumber=Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e){
                Log.error(StringValues.INIT_INVALID_PORT);
                return;
            }

            if(currentPortNumber<0||currentPortNumber>65535){
                Log.error(StringValues.INIT_INVALID_PORT);
                return;
            }

            if(i==1){
                port=currentPortNumber;
            }
            else{
                targetPorts.add(currentPortNumber);
            }
        }

        if(targetPorts.size()==0){
            Log.warn(StringValues.INIT_NO_TARGET_PORTS);
        }

        tlsProxy.start(port,targetPorts);
    }
}
