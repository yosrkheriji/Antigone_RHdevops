import React, { useState, useEffect } from 'react';
import {
  HiOutlinePlus,
  HiOutlinePencil,
  HiOutlineTrash,
  HiOutlineExclamation,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
} from 'react-icons/hi';
import { useConfirm } from '../hooks/useConfirm';
import ConfirmDialog from '../components/ui/ConfirmDialog';
import { useAuth } from '../context/AuthContext';
import { employeService } from '../api/employeService';
import { tacheObligatoireService, TacheObligatoireDTO } from '../api/tacheObligatoireService';
import { Employe } from '../types';
import Button from '../components/ui/Button';
import Modal from '../components/ui/Modal';
import Badge from '../components/ui/Badge';

const MONTHS_FR = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc'];

const RestrictionsCongesPage: React.FC = () => {
  const { confirmState, confirm, handleConfirm, handleCancel } = useConfirm();
  const { user } = useAuth();

  // ============ Tâches Obligatoires state ============
  const [tachesObl, setTachesObl] = useState<TacheObligatoireDTO[]>([]);
  const [tachesOblLoading, setTachesOblLoading] = useState(true);
  const [showTacheOblModal, setShowTacheOblModal] = useState(false);
  const [editingGroup, setEditingGroup] = useState<TacheObligatoireDTO[] | null>(null);
  const [editTacheOblForm, setEditTacheOblForm] = useState({ nom: '', employeIds: [] as string[], dates: [] as string[] });
  const [subordinates, setSubordinates] = useState<Employe[]>([]);
  const [tacheOblForm, setTacheOblForm] = useState({
    nom: '',
    employeIds: [] as string[],
    dates: [] as string[],
  });

  // Mini date-picker state for tache obl
  const [pickerYear, setPickerYear] = useState(new Date().getFullYear());
  const [pickerMonth, setPickerMonth] = useState(new Date().getMonth());

  useEffect(() => {
    loadTachesObl();
    if (user?.employeId) {
      employeService.getSubordinates(user.employeId)
        .then(res => setSubordinates(res.data.data || []))
        .catch(console.error);
    }
  }, [user?.employeId]);

  // ============ Tâches Obligatoires logic ============
  const loadTachesObl = async () => {
    try {
      const res = await tacheObligatoireService.getAll();
      setTachesObl(res.data.data || []);
    } catch { /* ignore */ } finally {
      setTachesOblLoading(false);
    }
  };

  const toggleEmployeId = (employeId: string) => {
    setTacheOblForm((f) => ({
      ...f,
      employeIds: f.employeIds.includes(employeId)
        ? f.employeIds.filter((id) => id !== employeId)
        : [...f.employeIds, employeId],
    }));
  };

  const togglePickerDate = (dateStr: string) => {
    setTacheOblForm((f) => ({
      ...f,
      dates: f.dates.includes(dateStr)
        ? f.dates.filter((d) => d !== dateStr)
        : [...f.dates, dateStr],
    }));
  };

  const handleSaveTacheObl = async () => {
    if (!tacheOblForm.nom.trim()) {
      alert('Veuillez renseigner le nom de la restriction.');
      return;
    }
    if (tacheOblForm.employeIds.length === 0) {
      alert('Veuillez sélectionner au moins un collaborateur.');
      return;
    }
    if (tacheOblForm.dates.length === 0) {
      alert('Veuillez sélectionner au moins une date dans le calendrier.');
      return;
    }
    try {
      for (const employeId of tacheOblForm.employeIds) {
        await tacheObligatoireService.create({
          nom: tacheOblForm.nom,
          equipeId: null as any,
          employeId: Number(employeId),
          dates: tacheOblForm.dates,
        });
      }
      setShowTacheOblModal(false);
      setTacheOblForm({ nom: '', employeIds: [], dates: [] });
      loadTachesObl();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erreur lors de la sauvegarde');
    }
  };

  const handleDeleteTacheObl = async (id: number) => {
    confirm('Supprimer cette restriction ?', async () => {
      try {
        await tacheObligatoireService.delete(id);
        loadTachesObl();
      } catch { /* ignore */ }
    }, 'Supprimer la restriction');
  };

  const handleDeleteGroup = async (records: TacheObligatoireDTO[]) => {
    confirm(`Supprimer la restriction "${records[0]?.nom}" et toutes ses entrées ?`, async () => {
      try {
        await Promise.all(records.map(r => tacheObligatoireService.delete(r.id)));
        loadTachesObl();
      } catch { /* ignore */ }
    }, 'Supprimer la restriction');
  };

  const openEditGroup = (records: TacheObligatoireDTO[]) => {
    setEditingGroup(records);
    const employeIds = [...new Set(records.filter(r => r.employeId).map(r => String(r.employeId)))];
    const dates = [...new Set(records.flatMap(r => r.dates))].sort();
    setEditTacheOblForm({ nom: records[0].nom, employeIds, dates });
    setPickerYear(new Date().getFullYear());
    setPickerMonth(new Date().getMonth());
  };

  const handleEditTacheOblSave = async () => {
    if (!editingGroup) return;
    if (!editTacheOblForm.nom.trim()) { alert('Veuillez renseigner le nom.'); return; }
    if (editTacheOblForm.employeIds.length === 0) { alert('Veuillez sélectionner au moins un collaborateur.'); return; }
    if (editTacheOblForm.dates.length === 0) { alert('Veuillez sélectionner au moins une date.'); return; }
    try {
      await Promise.all(editingGroup.map(r => tacheObligatoireService.delete(r.id)));
      for (const employeId of editTacheOblForm.employeIds) {
        await tacheObligatoireService.create({
          nom: editTacheOblForm.nom,
          equipeId: null as any,
          employeId: Number(employeId),
          dates: editTacheOblForm.dates,
        });
      }
      setEditingGroup(null);
      loadTachesObl();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erreur lors de la modification');
    }
  };

  const toggleEditDate = (dateStr: string) => {
    setEditTacheOblForm(f => ({ ...f, dates: f.dates.includes(dateStr) ? f.dates.filter(d => d !== dateStr) : [...f.dates, dateStr] }));
  };
  const toggleEditEmployeId = (employeId: string) => {
    setEditTacheOblForm(f => ({ ...f, employeIds: f.employeIds.includes(employeId) ? f.employeIds.filter(id => id !== employeId) : [...f.employeIds, employeId] }));
  };

  // Grouped view for table
  const groupedRestrictions = React.useMemo(() => {
    const map = new Map<string, TacheObligatoireDTO[]>();
    for (const t of tachesObl) {
      if (!map.has(t.nom)) map.set(t.nom, []);
      map.get(t.nom)!.push(t);
    }
    return Array.from(map.values()).map(records => ({
      nom: records[0].nom,
      equipeNoms: [...new Set(records.map(r => r.equipeNom))],
      membreNoms: [...new Set(records.filter(r => r.employeNom).map(r => r.employeNom!))],
      toutesEquipes: records.every(r => !r.employeId),
      dates: [...new Set(records.flatMap(r => r.dates))].sort(),
      records,
    }));
  }, [tachesObl]);

  // Mini picker helpers
  const pickerDays = () => {
    const days: (number | null)[] = [];
    const firstDay = new Date(pickerYear, pickerMonth, 1);
    const offset = (firstDay.getDay() + 6) % 7;
    const total = new Date(pickerYear, pickerMonth + 1, 0).getDate();
    for (let i = 0; i < offset; i++) days.push(null);
    for (let d = 1; d <= total; d++) days.push(d);
    return days;
  };
  const pickerDateStr = (day: number) =>
    `${pickerYear}-${String(pickerMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-title-sm font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-gray-900 to-[#683b77] dark:from-white dark:to-[#ab78c3]">
            Restrictions de congés
          </h1>
          <p className="text-theme-sm text-gray-500 dark:text-gray-400 mt-1">
            Gérer les jours de restriction de congé pour vos collaborateurs directs
          </p>
        </div>
        <Button onClick={() => { setShowTacheOblModal(true); setTacheOblForm({ nom: '', employeIds: [], dates: [] }); }}>
          <HiOutlinePlus size={18} /> Ajouter
        </Button>
      </div>

      {tachesOblLoading ? (
        <div className="text-center py-12 text-gray-500">Chargement...</div>
      ) : tachesObl.length === 0 ? (
        <div className="text-center py-12">
          <HiOutlineExclamation className="mx-auto text-yellow-400" size={40} />
          <p className="mt-3 text-gray-500">Aucune restriction configurée</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-gray-dark">
          <p className="px-4 pt-3 pb-1 text-theme-xs text-gray-400 italic">Double-cliquez sur une ligne pour modifier</p>
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-100 dark:border-gray-700">
                {['Restriction', 'Collaborateurs concernés', 'Dates', ''].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-theme-xs font-medium text-gray-500 uppercase">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {groupedRestrictions.map((g) => (
                <tr
                  key={g.nom}
                  onDoubleClick={() => openEditGroup(g.records)}
                  className="border-b border-gray-50 dark:border-gray-800 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                >
                  <td className="px-4 py-3 font-medium text-theme-sm text-gray-800 dark:text-white max-w-[160px] truncate">{g.nom}</td>
                  <td className="px-4 py-3">
                    {g.membreNoms.length > 0 ? (
                      <div className="flex flex-wrap gap-1">
                        {g.membreNoms.map(m => (
                          <span key={m} className="inline-flex items-center rounded-full bg-secondary-50 text-secondary-600 px-2 py-0.5 text-[10px] font-medium dark:bg-secondary-500/10">{m}</span>
                        ))}
                      </div>
                    ) : (
                      <span className="text-gray-400 italic text-theme-xs">Non spécifié</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="grid grid-cols-2 gap-x-2 gap-y-0.5">
                      {g.dates.slice(0, 4).map(d => (
                        <span key={d} className="inline-flex items-center rounded-full bg-yellow-100 text-yellow-700 px-2 py-0.5 text-[10px] font-medium dark:bg-yellow-500/20 dark:text-yellow-400 whitespace-nowrap">{d}</span>
                      ))}
                      {g.dates.length > 4 && <span className="col-span-2 text-theme-xs text-gray-400">+{g.dates.length - 4} autres</span>}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1" onClick={e => e.stopPropagation()}>
                      <button onClick={() => openEditGroup(g.records)} className="p-1.5 rounded-lg hover:bg-brand-50 text-brand-500" title="Modifier">
                        <HiOutlinePencil size={15} />
                      </button>
                      <button onClick={() => handleDeleteGroup(g.records)} className="p-1.5 rounded-lg hover:bg-error-50 text-error-500" title="Supprimer">
                        <HiOutlineTrash size={15} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Tâche Obligatoire Modal */}
      <Modal isOpen={showTacheOblModal} onClose={() => setShowTacheOblModal(false)} title="Nouvelle tâche obligatoire" size="lg">
        <div className="space-y-4">
          <div>
            <label className="block text-theme-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nom *</label>
            <input
              type="text"
              value={tacheOblForm.nom}
              onChange={(e) => setTacheOblForm((f) => ({ ...f, nom: e.target.value }))}
              placeholder="Ex: Déploiement sprint 3"
              className="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-theme-sm text-gray-700 focus:border-brand-300 focus:outline-none focus:ring focus:ring-brand-500/10 dark:border-gray-600 dark:text-gray-300"
            />
          </div>
          <div>
            <label className="block text-theme-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              Collaborateurs concernés * ({tacheOblForm.employeIds.length} sélectionné{tacheOblForm.employeIds.length > 1 ? 's' : ''})
            </label>
            {subordinates.length === 0 ? (
              <p className="text-theme-xs text-gray-400 italic px-2 py-3 border border-dashed border-gray-300 rounded-lg text-center">
                Aucun collaborateur sous votre responsabilité
              </p>
            ) : (
              <div className="max-h-44 overflow-y-auto rounded-lg border border-gray-300 dark:border-gray-600">
                {subordinates.map((s) => (
                  <label key={s.id} className={`flex cursor-pointer items-center gap-3 px-4 py-2 hover:bg-gray-50 dark:hover:bg-gray-800 ${tacheOblForm.employeIds.includes(String(s.id)) ? 'bg-secondary-50 dark:bg-secondary-500/10' : ''}`}>
                    <input
                      type="checkbox"
                      checked={tacheOblForm.employeIds.includes(String(s.id))}
                      onChange={() => toggleEmployeId(String(s.id))}
                      className="h-4 w-4 rounded text-secondary-500"
                    />
                    <span className="text-theme-sm text-gray-700 dark:text-gray-300">{s.prenom} {s.nom}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
          <div>
            <label className="block text-theme-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Sélectionner les jours obligatoires * <span className="text-theme-xs text-gray-400 font-normal">({tacheOblForm.dates.length} sélectionné{tacheOblForm.dates.length > 1 ? 's' : ''})</span></label>
            <div className="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900">
              <div className="flex items-center justify-between mb-3">
                <button type="button" onClick={() => { if (pickerMonth === 0) { setPickerMonth(11); setPickerYear(pickerYear - 1); } else setPickerMonth(pickerMonth - 1); }} className="p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-800">
                  <HiOutlineChevronLeft size={16} />
                </button>
                <span className="text-theme-sm font-semibold text-gray-800 dark:text-white">{MONTHS_FR[pickerMonth]} {pickerYear}</span>
                <button type="button" onClick={() => { if (pickerMonth === 11) { setPickerMonth(0); setPickerYear(pickerYear + 1); } else setPickerMonth(pickerMonth + 1); }} className="p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-800">
                  <HiOutlineChevronRight size={16} />
                </button>
              </div>
              <div className="grid grid-cols-7 gap-0.5 text-center">
                {['Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa', 'Di'].map((d) => (
                  <div key={d} className="text-theme-xs font-medium text-gray-400 py-1">{d}</div>
                ))}
                {pickerDays().map((day, i) => {
                  if (!day) return <div key={`e-${i}`} />;
                  const ds = pickerDateStr(day);
                  const selected = tacheOblForm.dates.includes(ds);
                  return (
                    <button
                      key={ds}
                      type="button"
                      onClick={() => togglePickerDate(ds)}
                      className={`w-8 h-8 mx-auto flex items-center justify-center rounded-md text-theme-xs font-medium transition-colors ${selected
                        ? 'bg-yellow-400 text-white dark:bg-yellow-500'
                        : 'hover:bg-yellow-50 text-gray-700 dark:text-gray-300 dark:hover:bg-yellow-500/20'
                        }`}
                    >
                      {day}
                    </button>
                  );
                })}
              </div>
            </div>
            {tacheOblForm.dates.length > 0 && (
              <div className="flex flex-wrap gap-1 mt-2">
                {tacheOblForm.dates.sort().map((d) => (
                  <span key={d} className="inline-flex items-center gap-1 rounded-full bg-yellow-100 text-yellow-700 px-2 py-0.5 text-[10px] font-medium dark:bg-yellow-500/20 dark:text-yellow-400">
                    {d}
                    <button type="button" onClick={() => togglePickerDate(d)} className="hover:text-yellow-900">×</button>
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-6">
          <Button variant="ghost" onClick={() => setShowTacheOblModal(false)}>Annuler</Button>
          <Button onClick={handleSaveTacheObl}>Créer</Button>
        </div>
      </Modal>

      {/* Edit Restriction Modal */}
      <Modal isOpen={!!editingGroup} onClose={() => setEditingGroup(null)} title="Modifier la restriction" size="lg">
        {editingGroup && (
          <div className="space-y-4 max-h-[70vh] overflow-y-auto pr-1">
            <div>
              <label className="block text-theme-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Nom *</label>
              <input
                type="text"
                value={editTacheOblForm.nom}
                onChange={e => setEditTacheOblForm(f => ({ ...f, nom: e.target.value }))}
                className="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 text-theme-sm text-gray-700 focus:border-brand-300 focus:outline-none focus:ring focus:ring-brand-500/10 dark:border-gray-600 dark:text-gray-300"
              />
            </div>
            <div>
              <label className="block text-theme-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Collaborateurs *
                <span className="text-theme-xs text-gray-400 font-normal ml-1">({editTacheOblForm.employeIds.length} sélectionné{editTacheOblForm.employeIds.length > 1 ? 's' : ''})</span>
              </label>
              {subordinates.length === 0 ? (
                <p className="text-theme-xs text-gray-400 italic px-2 py-3 border border-dashed border-gray-300 rounded-lg text-center">Aucun collaborateur disponible</p>
              ) : (
                <div className="max-h-44 overflow-y-auto rounded-lg border border-gray-300 dark:border-gray-600">
                  {subordinates.map(s => (
                    <label key={s.id} className={`flex cursor-pointer items-center gap-3 px-4 py-2 hover:bg-gray-50 dark:hover:bg-gray-800 ${editTacheOblForm.employeIds.includes(String(s.id)) ? 'bg-secondary-50 dark:bg-secondary-500/10' : ''}`}>
                      <input type="checkbox" checked={editTacheOblForm.employeIds.includes(String(s.id))} onChange={() => toggleEditEmployeId(String(s.id))} className="h-4 w-4 rounded text-secondary-500" />
                      <span className="text-theme-sm text-gray-700 dark:text-gray-300">{s.prenom} {s.nom}</span>
                    </label>
                  ))}
                </div>
              )}
            </div>
            {/* Date picker */}
            <div>
              <label className="block text-theme-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Dates <span className="text-theme-xs text-gray-400 font-normal">({editTacheOblForm.dates.length} sélectionnée{editTacheOblForm.dates.length > 1 ? 's' : ''})</span>
              </label>
              <div className="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900">
                <div className="flex items-center justify-between mb-3">
                  <button type="button" onClick={() => { if (pickerMonth === 0) { setPickerMonth(11); setPickerYear(pickerYear - 1); } else setPickerMonth(pickerMonth - 1); }} className="p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-800"><HiOutlineChevronLeft size={16} /></button>
                  <span className="text-theme-sm font-semibold text-gray-800 dark:text-white">{MONTHS_FR[pickerMonth]} {pickerYear}</span>
                  <button type="button" onClick={() => { if (pickerMonth === 11) { setPickerMonth(0); setPickerYear(pickerYear + 1); } else setPickerMonth(pickerMonth + 1); }} className="p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-800"><HiOutlineChevronRight size={16} /></button>
                </div>
                <div className="grid grid-cols-7 gap-0.5 text-center">
                  {['Lu', 'Ma', 'Me', 'Je', 'Ve', 'Sa', 'Di'].map(d => <div key={d} className="text-theme-xs font-medium text-gray-400 py-1">{d}</div>)}
                  {pickerDays().map((day, i) => {
                    if (!day) return <div key={`e-${i}`} />;
                    const ds = pickerDateStr(day);
                    const sel = editTacheOblForm.dates.includes(ds);
                    return <button key={ds} type="button" onClick={() => toggleEditDate(ds)} className={`w-8 h-8 mx-auto flex items-center justify-center rounded-md text-theme-xs font-medium transition-colors ${sel ? 'bg-yellow-400 text-white' : 'hover:bg-yellow-50 text-gray-700 dark:text-gray-300 dark:hover:bg-yellow-500/20'}`}>{day}</button>;
                  })}
                </div>
              </div>
              {editTacheOblForm.dates.length > 0 && (
                <div className="flex flex-wrap gap-1 mt-2">
                  {editTacheOblForm.dates.sort().map(d => (
                    <span key={d} className="inline-flex items-center gap-1 rounded-full bg-yellow-100 text-yellow-700 px-2 py-0.5 text-[10px] font-medium">
                      {d}<button type="button" onClick={() => toggleEditDate(d)}>×</button>
                    </span>
                  ))}
                </div>
              )}
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <Button variant="ghost" onClick={() => setEditingGroup(null)}>Annuler</Button>
              <Button onClick={handleEditTacheOblSave}>Enregistrer</Button>
            </div>
          </div>
        )}
      </Modal>

      <ConfirmDialog
        isOpen={confirmState.isOpen}
        title={confirmState.title}
        message={confirmState.message}
        confirmLabel="Supprimer"
        onConfirm={handleConfirm}
        onCancel={handleCancel}
      />
    </div>
  );
};

export default RestrictionsCongesPage;
