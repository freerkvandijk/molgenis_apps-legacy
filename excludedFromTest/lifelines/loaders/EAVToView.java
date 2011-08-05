package lifelines.loaders;

import app.JpaDatabase;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.organization.Investigation;
import org.molgenis.pheno.Measurement;

/**
 *
 * @author joris lops
 */
public class EAVToView {
    private final String schemaName;
    private final String tableName;
    private final int investigationId;
    private final String schemaToExportView;
    private final String databaseTarget;
    private final int protocolId;
    
    public EAVToView(String schemaName, String tableName, String schemaToExportView, int protocolId, String databaseTarget, int investigationId) throws DatabaseException, Exception {
    	this.schemaName = schemaName;
    	this.tableName = tableName;
    	this.schemaToExportView = schemaToExportView;
    	this.protocolId = protocolId;
    	this.databaseTarget = databaseTarget;
    	this.investigationId = investigationId;
    	load();
    }    
    
    private void load() throws DatabaseException, Exception {
    	
        JpaDatabase db = new JpaDatabase();
        EntityManager em = db.getEntityManager();
            
        String column = "max(case when feature = %d then %s end) as %s \n";
                
        StringBuilder query = new StringBuilder("SELECT ");    
        List<Measurement> measurements = LoaderUtils.getMeasurementsByInvestigationId(investigationId, em, this.schemaName, this.tableName);
        for(int i = 0; i < measurements.size(); ++i) {
            Measurement m = measurements.get(i);
            String castPart = LoaderUtils.getCast(m.getDataType());
            if(databaseTarget.equals("mysql")) {
            	if(castPart.contains("number")) {
            		castPart = castPart.replace("number", "DECIMAL");
            	} else if(castPart.contains("to_date")) {
            		castPart = "CAST(substr(value,1, 19) AS DATETIME)";
            	}
            }
            query.append(String.format(column, m.getId(), String.format(castPart, "value"), m.getName()));
            if(i + 1 < measurements.size()) {
                query.append(",");
            }
        }        
        query.append(String.format(" FROM \n observedvalue \n WHERE investigation = %d AND protocolId = %d \n GROUP BY recordId", investigationId, protocolId));
        
        

        String viewQuery = "";
        
    	String tempTableName = (schemaToExportView != null) ? "" + schemaToExportView + "." : "";
    	tempTableName += tableName;        
        if(databaseTarget.equals("mysql")) {
        	viewQuery = String.format("CREATE TABLE %s %S", tempTableName, query.toString()); 
        } else {
        	//String materializedView = "CREATE MATERIALIZED VIEW %s NOCACHE NOPARALLEL BUILD IMMEDIATE USING INDEX REFRESH ON DEMAND COMPLETE DISABLE QUERY REWRITE AS %s";
        	String view = "CREATE VIEW %s AS %s";
        	viewQuery = String.format(view, tempTableName, query.toString());;
        }
        
        System.out.println();
        System.out.println(viewQuery.toString());
//        Object result = db.getEntityManager().createNativeQuery(viewQuery).getResultList();
//        if(result != null)
//        	System.out.println("view created");
//        else
//        	System.out.println("view created failed!");
        
        
        Session s = db.getEntityManager().unwrap(Session.class);
        Transaction t = s.beginTransaction();
        if(databaseTarget.equals("mysql")) {
        	s.createSQLQuery(String.format("DROP TABLE IF EXISTS %s", tableName)).executeUpdate();
        } else {
        	//figure out if it exists first
        	//s.createSQLQuery(String.format("DROP TABLE %s", tableName)).executeUpdate();
        }
        
        s.createSQLQuery(viewQuery).executeUpdate();
        t.commit();
        
        
    }
}
