import pandas as pd
from sklearn.preprocessing import StandardScaler
import pickle

if __name__ == '__main__':
    df = pd.read_csv('scores.csv', delim_whitespace=False)
    X = df.drop(labels=['ID'], axis='columns')

    scaler = StandardScaler()
    scaler.fit(X)
    X = scaler.transform(X)

    with open('rf.model', 'rb') as f:
        model = pickle.load(f)

    print(model.predict(X))

# Ali Ghanbari (alig@iastate.edu)
