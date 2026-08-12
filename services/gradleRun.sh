for service in apigatewayservice authservice foodservice orderingservice paymentservice serviceregistry; 
do
    echo "========================================"
    echo "BUILDING $service"
    echo "========================================"
    (cd $service && ./gradlew clean build -x test)
done
