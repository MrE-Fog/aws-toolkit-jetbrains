using Amazon.Lambda.Core;
using Amazon.Lambda.APIGatewayEvents;

[assembly: LambdaSerializer(typeof(Amazon.Lambda.Serialization.SystemTextJson.DefaultLambdaJsonSerializer))]

namespace HelloWorld
{
    public class Function
    {
        public string FunctionHandler(APIGatewayProxyRequest apigProxyEvent, ILambdaContext context)
        {
            return "Return string".ToUpper();
        }
    }
}