package simpledb;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Lab3Main {

    public static void main(String[] argv) 
       throws DbException, TransactionAbortedException, IOException {
    	
    	
    	
        System.out.println("Loading schema from file:");
        // file named college.schema must be in mysimpledb directory
        Database.getCatalog().loadSchema("college.schema");

        // SQL query: SELECT * FROM STUDENTS WHERE name="Alice"
        // algebra translation: select_{name="alice"}( Students )
        // query plan: a tree with the following structure
        // - a Filter operator is the root; filter keeps only those w/ name=Alice
        // - a SeqScan operator on Students at the child of root
        TransactionId tid = new TransactionId();
        SeqScan scanStudents = new SeqScan(tid, Database.getCatalog().getTableId("students"));
        SeqScan scanTakes = new SeqScan(tid, Database.getCatalog().getTableId("takes"));
        SeqScan scanProfs = new SeqScan(tid, Database.getCatalog().getTableId("profs"));
        
        JoinPredicate jp1 = new JoinPredicate(1, Predicate.Op.EQUALS, 2);
        Join j1 = new Join(jp1, scanTakes, scanProfs);
        
        Predicate p = new Predicate(3, Predicate.Op.EQUALS, new StringField("hay", Type.STRING_LEN));
        Filter f = new Filter(p, j1);
        
        JoinPredicate jp2 = new JoinPredicate(0, Predicate.Op.EQUALS, 0);
        Join j2 = new Join(jp2, scanStudents, f);
        
        ArrayList<Type> types = new ArrayList<Type>();
        types.add(Type.STRING_TYPE);
        ArrayList<Integer> fields = new ArrayList<Integer>();
        fields.add(1);
        Project proj = new Project(fields, types, j2);
        
        // query execution: we open the iterator of the root and iterate through results
        System.out.println("Query results:");
        proj.open();
        while (proj.hasNext()) {
            Tuple tup = proj.next();
            System.out.println("\t"+tup);
        }
        proj.close();
        Database.getBufferPool().transactionComplete(tid);
    }

}