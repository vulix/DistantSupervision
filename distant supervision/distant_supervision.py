import nltk
import os
import glob
import networkx as nx
import numpy as np
from itertools import tee,izip
from sklearn import linear_model 
from nltk.probability import FreqDist
from sklearn import cross_validation
from sklearn.naive_bayes import MultinomialNB 
from sklearn.metrics import classification_report

#Assume the python script is stored at the same level of the data file folder
current=os.getcwd()
rel_path="/project3_data/"
abs_file_dir=current+rel_path
print abs_file_dir

# get the training instances and test instances given a relation name
def prepare_data(relation_name,abs_file_dir):
	training_instances=[]
	test_instances=[]
	for root, dirs, files in os.walk(abs_file_dir):
		for file in files:
			if file.startswith(relation_name):
				file_in=open(abs_file_dir+file,"r")
				corpus=file_in.read()
				instances=corpus.split("\n\n")
				if file.endswith('.train'):
					for i in instances:
						training_instances.append(i)
				if file.endswith('.test'):
					for i in instances:
						test_instances.append(i)
	return (training_instances, test_instances)

(training_instances,test_instances)=prepare_data('org_country_of_headquarters',abs_file_dir)

# generate a list of tuples given a window size and a sentence
def window(iterable, size):
	iters = tee(iterable, size)
	for i in xrange(1, size):
		for each in iters[i:]:
			next(each, None)
	return izip(*iters)

#generate dependency graph
def construct_dependency_graph(dependency):
	f = lambda A, n=3: [A[i:i+n] for i in range(0, len(A), n)]
	graph_input=f(dependency)
	G=nx.Graph()
	bundle={}
	for edge in graph_input:
		bundle[(int(edge[0]),int(edge[1]))]=edge[2]
	G.add_edges_from(bundle.keys())
	return (G,bundle)

# look for an entity in a sentence, return the index of the entity in that sentence
def look_for_entity_in_sentence(words,entity_list,window_size):
	
	index_dict={}
	if window_size>1:
		temp=window(words, window_size)
		for idx,value in enumerate(temp):
			index=[]
			if index_dict.get(value)!=None:
				index=index_dict.get(value)
			index.append(idx)
			index_dict[value]=index
		
		if len(entity_list)!=window_size:
			return index_dict.get(tuple(entity_list.split(' ')),[])
		else:
			return index_dict.get(tuple(entity_list),[])
	if window_size==1:
		for idx, value in enumerate(words):
			index=[]
			if index_dict.get(value)!=None:
				index=index_dict.get(value)
			index.append(idx)
			index_dict[value]=index
		
		if len(entity_list)>1:
			return index_dict.get(entity_list,[])
		else:
			return index_dict.get(entity_list[0],[])

# look for named entities given an input tag, return the index of entities with the same tag in the sentence
def look_for_ner_in_sentence(ner,ner_tag):
	index_dict={}
	for idx, value in enumerate(ner):
		index=[]
		if index_dict.get(value)!=None:
			index=index_dict.get(value)
		index.append(idx)
		index_dict[value]=index
	ner_list=index_dict.get(ner_tag)
	return ner_list

# find entity pairs: iput the tokens, dependency graph, entity and slot
# return entity index, slot index, pos tags, name entity tags and words in the sentence 
def entity_pair_extraction(tokens,dependency,entity_list,slot_list):
	words=['#WORD']
	pos=['#POS']
	ner=['#NER']
	for idx,token in enumerate(tokens[1:]):
		a=token.rindex('/')
		ner.append(token[a+1:])
		rest=token[:a]
		b=rest.rindex('/')
		pos.append(rest[b+1:])
		words.append(rest[:b])

	entity_idx= look_for_entity_in_sentence(words,entity_list,len(entity_list))
	slot_idx=look_for_entity_in_sentence(words,slot_list,len(slot_list))
	if len(slot_idx)>0:
		for loc in entity_idx:
			for i in range(len(entity_list)):
				if loc+i in slot_idx:
					slot_idx.remove(loc+i)

	other_slot_idx=[]
	if len(slot_idx)>0:
		idx=slot_idx[0]
		other_slot_idx=look_for_ner_in_sentence(ner,ner[idx])
		for loc in slot_idx:
			for i in range(len(slot_list)):
				if loc+i in other_slot_idx:
					other_slot_idx.remove(loc+i)
		for loc in entity_idx:
			for i in range(len(entity_list)):
				if loc+i in other_slot_idx:
					other_slot_idx.remove(loc+i)
 	return (entity_idx,slot_idx,other_slot_idx,words,pos,ner)

# extract lexical and syntactic features
def feature_extraction(words,pos,ner,shortest_path,bundle):
	lex_features=''
	syn_features=''
	if shortest_path[0]>shortest_path[-1]:
		lex_features+='R '
		syn_features+='R '
	else:
		lex_features+='L '
		syn_features+='L '
	lex_features+=ner[shortest_path[0]]+' '
	for node in shortest_path[1:-1]:
		lex_features+=words[node]+'/'+pos[node]+' '
	lex_features+=ner[shortest_path[-1]]
	for i in range(len(shortest_path)-1):
		lex=''
		dep=''
		direction=''
		if i==0:
			lex=ner[shortest_path[i]]
		else:
			lex=words[shortest_path[i]]
		if bundle.get((shortest_path[i],shortest_path[i+1]))!=None:
			dep=bundle.get((shortest_path[i],shortest_path[i+1]))
			direction='>'
		else:
			dep=bundle.get((shortest_path[i+1],shortest_path[i]))
			direction='<'
		syn_features+=lex+' '+dep+direction+' ' 
	syn_features+=ner[shortest_path[-1]]
	return (lex_features,syn_features)

# get shortest depenency path between an entity pair
def generate_shortest_path(p,entity_idx,slot_idx,entity_list,slot_list):
	shortest_path=[]
	for e in entity_idx:
		for i in range(len(entity_list)):
			for s in slot_idx:
				for j in range(len(slot_list)):
					if p.get(e+i)!=None:
						if p[e+i].get(s+j)!=None:
							if len(shortest_path)==0:
								shortest_path=p[e+i][s+j]
							else:
								if len(p[e+i][s+j])<len(shortest_path):
									shortest_path=p[e+i][s+j]
	return shortest_path

# generate positive instances, negative instances, and feature set 
# given a shortest path threshold and all the instances 
def single_instance_generation(instances, shortest_path_threshold):
	feature_set=set()
	pos_instances=dict()
	neg_instances=dict()
	for instance in instances:
		lines=instance.decode('utf-8').lower().split("\n")
		entity=''
		slot=''
		if len(lines)>2:				
			entity=lines[0][8:]
			slot=lines[1][6:]
			pos_features=dict()
			
			entity_list=nltk.word_tokenize(entity)
			slot_list=nltk.word_tokenize(slot)
			tokens=[]
			dependency=[]
			for idx, line in enumerate(lines[2:]):
				if idx%2==0:
					tokens=line.split(' ')
				if idx%2==1:
					dependency=line[14:].split(' ')
					(entity_idx,slot_idx,other_slot_idx,words,pos,ner)=entity_pair_extraction(tokens,dependency,entity_list,slot_list)
					G=nx.Graph()
					(G,bundle)=construct_dependency_graph(dependency)
					p=nx.shortest_path(G)
					shortest_path=generate_shortest_path(p,entity_idx,slot_idx,entity_list,slot_list)
					
					if len(shortest_path)>0:
						(pos_lex_features,pos_syn_features)=feature_extraction(words,pos,ner,shortest_path,bundle)
						if pos_features.get(pos_lex_features)!=None:
							pos_features[pos_lex_features]=pos_features.get(pos_lex_features)+1
						else:
							pos_features[pos_lex_features]=1
						if pos_features.get(pos_syn_features)!=None:
							pos_features[pos_syn_features]=pos_features.get(pos_syn_features)+1
						else:
							pos_features[pos_syn_features]=1
						feature_set.add(pos_syn_features)
						feature_set.add(pos_lex_features)
					if len(other_slot_idx)>0:
						other_slot=[]
						slot_length=[]
						for i in range(len(other_slot_idx)):
							if i==0:
								other_slot.append(other_slot_idx[i])
								slot_length.append(1)
							else:
								if (other_slot_idx[i]-other_slot_idx[i-1])>1:
									other_slot.append(other_slot_idx[i])
									slot_length.append(1)
								if (other_slot_idx[i]-other_slot_idx[i-1])==1:
									slot_length[-1]+=1
						for idx, value in enumerate(other_slot):
							neg_features=dict()
							neg_slot=words[value:value+slot_length[idx]]
							neg_shortest_path=generate_shortest_path(p,entity_idx,other_slot[idx:idx+1],entity_list,neg_slot)
							
							if neg_instances.get((entity,' '.join(neg_slot)))!=None:
								neg_features=neg_instances.get((entity,' '.join(neg_slot)))
							if len(neg_shortest_path)>0 and len(neg_shortest_path)<shortest_path_threshold:
								(neg_lex_features,neg_syn_features)=feature_extraction(words,pos,ner,neg_shortest_path,bundle)
								if neg_features.get(neg_lex_features)!=None:
									neg_features[neg_lex_features]=neg_features.get(neg_lex_features)+1
								else:
									neg_features[neg_lex_features]=1
								if neg_features.get(neg_syn_features)!=None:
									neg_features[neg_syn_features]=neg_features.get(neg_syn_features)+1
								else:
									neg_features[neg_syn_features]=1
								feature_set.add(neg_syn_features)
								feature_set.add(neg_lex_features)
								neg_instances[(entity, ' '.join(neg_slot))]=neg_features
				pos_instances[(entity,slot)]=pos_features
	print len(pos_instances.keys())
	print len(neg_instances.keys())
	return (pos_instances, neg_instances,feature_set)

# generate feature vector X and label vector Y 
# given positive instances, negative instances and feature set
def feature_generation(pos_instances,neg_instances,feature_set):
	feature_list=list(feature_set)
	print len(feature_list)
	feature_dict= dict()
	for idx, value in enumerate(feature_list):
		feature_dict[value]=idx
	X=[]
	Y=[]
	for key,value in pos_instances.items():
		Y.append(1)
		features=[0]*len(feature_list)
		for feature,count in value.items():
			if feature_dict.get(feature)!=None:
				idx=feature_dict.get(feature)
				features[idx]=count
		X.append(features)
	for key, value in neg_instances.items():
		Y.append(0)
		features=[0]*len(feature_list)
		for feature,count in value.items():
			if feature_dict.get(feature)!=None:
				idx=feature_dict.get(feature)
				features[idx]=count
		X.append(features)
	return (X,Y)
	
# iteratively update training labels
def update_labels(X,Y,iteration):
	Ynew=np.asarray(Y[:])
	Xnew=np.asarray(X)
	target_names = ['class 0', 'class 1']
	for i in range(iteration):
		print i
		clf1=MultinomialNB()
		clf2=linear_model.LogisticRegression()
		predicted1 = cross_validation.cross_val_predict(clf1,Xnew,Ynew, cv=10)
		predicted2 = cross_validation.cross_val_predict(clf2,Xnew,Ynew, cv=10)
		for idx,value in enumerate(predicted1):
			if value==predicted2[idx]:
				Ynew[idx]=value
		print classification_report(Ynew, predicted1, target_names=target_names)
	return (X,Ynew)

# Prepare data for training   
(pos_instances, neg_instances,feature_set)=single_instance_generation(training_instances,4)
(X,Y)=feature_generation(pos_instances,neg_instances,feature_set)

# Prepare data for testing
(t_pos_instances, t_neg_instances,t_feature_set)=single_instance_generation(test_instances,4)
(testX,testY)=feature_generation(t_pos_instances,t_neg_instances,feature_set)

# Use Naive Bayes method to do relation classification
clf = MultinomialNB()
clf.fit(X, Y)
result1=clf.predict(testX)
# Generate classification report (precision, recall, F1 and support) by classes
target_names = ['class 0', 'class 1']
print classification_report(testY, result1, target_names=target_names)

# Update training labels to reduce FN and FP
(X,Ynew)=update_labels(X,Y,5)
clf.fit(X,Ynew)
result2=clf.predict(testX)
print classification_report(testY, result2, target_names=target_names)

# significance test 
outperform_count=0
for i in range(10000):
	
	sample=np.random.choice(len(result1),20)
	old=0
	new=0
	for idx in sample:
		if result1[idx]==testY[idx]:
			old+=1
		if result2[idx]==testY[idx]:
			new+=1
	if old<new:
		outperform_count+=1

print 1.0*outperform_count/10000





