// создаем генератор
CREATE SEQUENCE GENERATORID;
ALTER SEQUENCE GENERATORID RESTART WITH 0;

// создаем триггера дл€ автоинкремента
CREATE OR ALTER TRIGGER TYPES_AUTOINCREMENT FOR TYPES
ACTIVE BEFORE INSERT POSITION 0
as
begin
  if ((new.TYPE_ID is null) or (new.TYPE_ID = 0)) then
  begin
    new.TYPE_ID = gen_id(GENERATORID, 1);
  end
end
^
SET TERM ; ^

// пример использовани€
// insert into types (title_type, cat) values('Smth', '/category')
// триггер дл€ сохранени€ целостности
CREATE OR ALTER TRIGGER DOC_DELETE FOR DOCUMENTS
ACTIVE AFTER DELETE POSITION 0
AS
begin
  delete from deals where d_id=old.doc_id;
end
^
SET TERM ; ^
CREATE OR ALTER TRIGGER DOC_UPDATE FOR DOCUMENTS
ACTIVE AFTER UPDATE POSITION 0
as
begin
  if (old.doc_id <> new.doc_id) then
  begin
    update DEALS
    set D_ID = new.DOC_ID
    where D_ID = old.DOC_ID;
  end
end
^
SET TERM ; ^

