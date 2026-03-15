#!/bin/bash
# ============================================================
# Create Kafka topics for Clinical Order Management System
# Run this after Kafka is up: ./scripts/create-topics.sh
# ============================================================

KAFKA_BOOTSTRAP="localhost:9092"

echo "Creating Kafka topics..."

docker exec clinical-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --create --if-not-exists \
  --topic clinical-order-created \
  --partitions 3 \
  --replication-factor 1

docker exec clinical-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --create --if-not-exists \
  --topic medication-reserved \
  --partitions 3 \
  --replication-factor 1

# Dead letter queues (for future use)
docker exec clinical-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --create --if-not-exists \
  --topic clinical-order-created.DLT \
  --partitions 1 \
  --replication-factor 1

docker exec clinical-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --create --if-not-exists \
  --topic medication-reserved.DLT \
  --partitions 1 \
  --replication-factor 1

echo ""
echo "Topics created. Listing all topics:"
docker exec clinical-kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server $KAFKA_BOOTSTRAP \
  --list
