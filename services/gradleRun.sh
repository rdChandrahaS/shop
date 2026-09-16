for service in apigatewayservice authservice foodservice orderingservice paymentservice serviceregistry messageservice; 
do
    echo "========================================"
    echo "BUILDING $service"
    echo "========================================"
    (cd $service && ./gradlew clean build -x test)
done
