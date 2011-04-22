SET TERM ^ ;
create or alter procedure NEW_PROCEDURE (
    WORKER integer,
    SEAT integer)
as
declare variable WORKERS_IDS integer;
begin
  for select WORK_ID
      from WORKERS
      where S_ID = :SEAT
      into  :WORKERS_IDS
      do
      update meetings set W_MAN = :worker where W_MAN= :workers_ids;
end^

SET TERM ; ^
GRANT SELECT ON WORKERS TO PROCEDURE NEW_PROCEDURE;
GRANT SELECT,UPDATE ON MEETINGS TO PROCEDURE NEW_PROCEDURE;
GRANT EXECUTE ON PROCEDURE NEW_PROCEDURE TO SYSDBA;
