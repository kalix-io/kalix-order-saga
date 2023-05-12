# Kalix Order Saga

## Designing

To understand the Kalix concepts that are the basis for this example, see [Designing services](https://docs.kalix.io/java/development-process.html) in the documentation.

## Developing

This project demonstrates the use of Event Sourced Entities, Subscriptions and Actions components.
To understand more about these components, see [Developing services](https://docs.kalix.io/services/)
and in particular the [Java section](https://docs.kalix.io/java/)

## Building

Use Maven to build your project:

```shell
mvn compile
```

## Running Locally

When running a Kalix application locally, at least two applications are required. The current Kalix application and its companion Kalix Proxy.

To start the applications locally, call the following command:

```shell
mvn kalix:runAll
```

This command will start your Kalix application and a Kalix Proxy using the included [docker-compose.yml](.
/docker-compose.yml) file. 

By default, an `orchestration` Spring profile is enabled, to demonstrate 
orchestration-based Saga with the [Workflow Entity](https://docs.kalix.io/java/workflow-entities.html). Switching to 
choreography based Saga is just a matter of changing the profile configuration `-Dspring.profiles.active` to 
`choreography` in `pom.xml` file.

## Exercising the service

- Add product to the inventory

```shell
curl -XPATCH -H "Content-Type: application/json" \
  --data '{"productId": "p1", "quantity": 50}' \
  localhost:9000/inventory/global-inventory/add 
```

- Place an order

```shell
curl -XPOST -H "Content-Type: application/json" \
  --data '{"userId": "u1", "productId": "p1", "quantity": 10, "price": 10}' \
  localhost:9000/order/123 
```

- Check order status

```shell
curl localhost:9000/order/123 
```

- Check remaining stocks

```shell
curl localhost:9000/inventory/global-inventory/product/p1 
```

- Place an order with payments failure. Payment will fail for an order with an `id` set to `42`. 

```shell
curl -XPOST -H "Content-Type: application/json" \
  --data '{"userId": "u1", "productId": "p1", "quantity": 10, "price": 10}' \
  localhost:9000/order/42 
```

- Check order status

```shell
curl localhost:9000/order/42 
```

## Testing

There are two integration tests: `OrderWorkflowIntegrationTest` and `OrderEntityIntegrationTest` to validate orchestration and choreography-based Sagas.