sudo java -jar /home/fotis/workspace/db-umls-mapping.jar /home/fotis/workspace/DDI\ Prediction/MapDrugbankUMLS/ /home/fotis/workspace/DDI\ Prediction/ProcessNeo4j/ 
sudo java -jar /home/fotis/workspace/paths-extraction.jar /home/fotis/workspace/DDI\ Prediction/ProcessNeo4j/PositivePairs/ /var/lib/neo4j/data/databases/graph.db
sudo java -jar /home/fotis/workspace/paths-extraction.jar /home/fotis/workspace/DDI\ Prediction/ProcessNeo4j/NegativePairs/ /var/lib/neo4j/data/databases/graph.db
java -jar /home/fotis/workspace/feature-extraction.jar
python ./workspace/DDI\ Prediction/PythonClassifiers/random-forest-DDIs-approachB.py
