import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from sklearn.tree import DecisionTreeClassifier
from sklearn.model_selection import train_test_split
from sklearn import metrics

from sklearn.tree import export_graphviz
from sklearn.externals.six import StringIO  
from IPython.display import Image  
import pydotplus
import collections

data = pd.read_csv("/home/fot/workspace/python/approachb-features-equalized-merged.csv")
#print(data.head())


#plot data
#plt.scatter(data["math"], data["id"], color="blue")
#plt.xlabel("MATH")
#plt.ylabel("ID")
#plt.show()

#FOTIS: need to correct below...

#train model

feature_cols=["rel1_ADMINISTERED_TO", "rel1_AFFECTS", "rel1_ASSOCIATED_WITH", "rel1_AUGMENTS", "rel1_CAUSES", "rel1_COEXISTS_WITH", "rel1_compared_with", "rel1_COMPLICATES", "rel1_CONVERTS_TO", "rel1_DIAGNOSES", "rel1_different_from", "rel1_different_than", "rel1_DISRUPTS", "rel1_higher_than", "rel1_INHIBITS", "rel1_INTERACTS_WITH", "rel1_IS_A", "rel1_ISA", "rel1_LOCATION_OF", "rel1_lower_than", "rel1_MANIFESTATION_OF", "rel1_METHOD_OF", "rel1_OCCURS_IN", "rel1_PART_OF", "rel1_PRECEDES", "rel1_PREDISPOSES", "rel1_PREVENTS", "rel1_PROCESS_OF", "rel1_PRODUCES", "rel1_same_as", "rel1_STIMULATES", "rel1_TREATS", "rel1_USES", "rel1_MENTIONED_IN", "rel1_HAS_MESH", "rel2_ADMINISTERED_TO", "rel2_AFFECTS", "rel2_ASSOCIATED_WITH", "rel2_AUGMENTS", "rel2_CAUSES", "rel2_COEXISTS_WITH", "rel2_compared_with", "rel2_COMPLICATES", "rel2_CONVERTS_TO", "rel2_DIAGNOSES", "rel2_different_from", "rel2_different_than", "rel2_DISRUPTS", "rel2_higher_than", "rel2_INHIBITS", "rel2_INTERACTS_WITH", "rel2_IS_A", "rel2_ISA", "rel2_LOCATION_OF", "rel2_lower_than", "rel2_MANIFESTATION_OF", "rel2_METHOD_OF", "rel2_OCCURS_IN", "rel2_PART_OF", "rel2_PRECEDES", "rel2_PREDISPOSES", "rel2_PREVENTS", "rel2_PROCESS_OF", "rel2_PRODUCES", "rel2_same_as", "rel2_STIMULATES", "rel2_TREATS", "rel2_USES", "rel2_MENTIONED_IN", "rel2_HAS_MESH", "rel3_ADMINISTERED_TO", "rel3_AFFECTS", "rel3_ASSOCIATED_WITH", "rel3_AUGMENTS", "rel3_CAUSES", "rel3_COEXISTS_WITH", "rel3_compared_with", "rel3_COMPLICATES", "rel3_CONVERTS_TO", "rel3_DIAGNOSES", "rel3_different_from", "rel3_different_than", "rel3_DISRUPTS", "rel3_higher_than", "rel3_INHIBITS", "rel3_INTERACTS_WITH", "rel3_IS_A", "rel3_ISA", "rel3_LOCATION_OF", "rel3_lower_than", "rel3_MANIFESTATION_OF", "rel3_METHOD_OF", "rel3_OCCURS_IN", "rel3_PART_OF", "rel3_PRECEDES", "rel3_PREDISPOSES", "rel3_PREVENTS", "rel3_PROCESS_OF", "rel3_PRODUCES", "rel3_same_as", "rel3_STIMULATES", "rel3_TREATS", "rel3_USES", "rel3_MENTIONED_IN", "rel3_HAS_MESH", "rel4_ADMINISTERED_TO", "rel4_AFFECTS", "rel4_ASSOCIATED_WITH", "rel4_AUGMENTS", "rel4_CAUSES", "rel4_COEXISTS_WITH", "rel4_compared_with", "rel4_COMPLICATES", "rel4_CONVERTS_TO", "rel4_DIAGNOSES", "rel4_different_from", "rel4_different_than", "rel4_DISRUPTS", "rel4_higher_than", "rel4_INHIBITS", "rel4_INTERACTS_WITH", "rel4_IS_A", "rel4_ISA", "rel4_LOCATION_OF", "rel4_lower_than", "rel4_MANIFESTATION_OF", "rel4_METHOD_OF", "rel4_OCCURS_IN", "rel4_PART_OF", "rel4_PRECEDES", "rel4_PREDISPOSES", "rel4_PREVENTS", "rel4_PROCESS_OF", "rel4_PRODUCES", "rel4_same_as", "rel4_STIMULATES", "rel4_TREATS", "rel4_USES", "rel4_MENTIONED_IN", "rel4_HAS_MESH"]
X=data[feature_cols]
y=data["INTERACTS"]
# Split dataset into training set and test set
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.1) # 70% training and 30% test

#Create a Gaussian Classifier
clf=DecisionTreeClassifier(criterion="entropy", max_depth=5)

#Train the model using the training sets y_pred=clf.predict(X_test)
clf.fit(X_train,y_train)

y_pred=clf.predict(X_test)

print("classifier finished")


    
#print accuracy
# Model Accuracy, how often is the classifier correct?
print("Accuracy:",metrics.accuracy_score(y_test, y_pred))
print("Precision:",metrics.precision_score(y_test, y_pred))
print("Recall:",metrics.recall_score(y_test, y_pred))
    

#visualize tree
dot_data = StringIO()
export_graphviz(clf, out_file=dot_data,  
                filled=True, rounded=True,
                special_characters=True,feature_names = feature_cols,class_names=['NO_INTERACTION','INTERACTS'])
graph = pydotplus.graph_from_dot_data(dot_data.getvalue())


#added this code to color edges
colors = ('red', 'green', 'white')
nodes = graph.get_node_list()
for node in nodes:
    if node.get_name() not in ('node', 'edge'):
        values = clf.tree_.value[int(node.get_name())][0]
        #color only LEAF nodes with one class winning
        if max(values) > sum(values)/2:    
            node.set_fillcolor(colors[np.argmax(values)])
        #nodes with same values for both classes get the default color
        else:
            node.set_fillcolor(colors[-1])
#end of addition


        
graph.write_png('decisiontree.png')
Image(graph.create_png())
print('Generated DT in decisiontree.png...')
